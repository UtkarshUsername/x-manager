package app.xmanager.followlog

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import app.xmanager.followlog.sharing.R
import java.util.concurrent.Executors

private const val SAVED_AT = "savedAt"

class SharePostActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val savedAt = System.currentTimeMillis()
    val text = runCatching {
      intent.takeIf { it.action == Intent.ACTION_SEND && it.type == "text/plain" }
        ?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
    }.getOrNull()

    // Theme.NoDisplay requires finishing before Android resumes the activity.
    try {
      sendBroadcast(Intent(this, SaveSharedPostReceiver::class.java).apply {
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(SAVED_AT, savedAt)
      })
    } finally {
      finish()
    }
  }
}

class SaveSharedPostReceiver : BroadcastReceiver() {
  companion object {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
  }

  override fun onReceive(context: Context, intent: Intent) {
    val pending = goAsync()
    val app = context.applicationContext
    val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
    val savedAt = intent.getLongExtra(SAVED_AT, System.currentTimeMillis())
    worker.execute {
      val message = try {
        when (val result = parseShare(text)) {
          is ShareResult.Post -> {
            if (FollowLogStore.save(app, result, savedAt)) R.string.x_manager_post_saved
            else R.string.x_manager_post_duplicate
          }
          ShareResult.Rejected.MISSING_HANDLE -> R.string.x_manager_post_missing_handle
          ShareResult.Rejected.UNSUPPORTED -> R.string.x_manager_post_unsupported
          ShareResult.Rejected.AMBIGUOUS -> R.string.x_manager_post_ambiguous
        }
      } catch (_: Exception) {
        R.string.x_manager_post_save_failed
      }

      main.post {
        try {
          Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
          if (message == R.string.x_manager_post_saved) FollowLogStore.listeners.forEach { it() }
        } finally {
          pending.finish()
        }
      }
    }
  }
}
