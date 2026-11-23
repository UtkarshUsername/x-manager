package app.xmanager.scrolltimer

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

private const val X_PACKAGE = "com.twitter.android"
private const val SECOND_MS = 1_000L

class ScrollTimerService : AccessibilityService() {
  companion object {
    internal var running: ScrollTimerService? = null
      private set
  }

  private sealed interface Focus {
    data class XWindow(val id: Int, val bounds: Rect) : Focus
    data object Transient : Focus
    data object OtherApp : Focus
  }

  private val settings by lazy { ScrollTimerSettings(this) }
  private val overlay by lazy { TimerOverlay(this, settings, ::lockX, ::unlockX) }
  private val power by lazy { getSystemService(PowerManager::class.java)!! }
  private val keyguard by lazy { getSystemService(KeyguardManager::class.java)!! }
  private val shareSheetPackages by lazy {
    val resolved = packageManager.resolveActivity(Intent(Intent.ACTION_CHOOSER), PackageManager.MATCH_DEFAULT_ONLY)
    setOf("android", "com.android.intentresolver", "com.google.android.intentresolver", resolved?.activityInfo?.packageName)
  }
  private val handler = Handler(Looper.getMainLooper())
  private val tick = Runnable { refreshWindow() }
  private var receiverRegistered = false
  private var window: Focus.XWindow? = null
  private var session: ScrollClock? = null

  internal var sawScrollEvents = false
    private set
  internal var overlayFailed = false
    private set

  private val screenReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == Intent.ACTION_SCREEN_OFF) endSession() else refreshWindow()
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    if (!settings.enabled) {
      disableSelf()
      return
    }
    running = this
    if (!receiverRegistered) {
      registerReceiver(screenReceiver, IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_USER_PRESENT)
      })
      receiverRegistered = true
    }
    refreshWindow()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (running !== this) return
    when (event.eventType) {
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
      AccessibilityEvent.TYPE_WINDOWS_CHANGED -> refreshWindow()
      AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
        if (event.packageName?.toString() != X_PACKAGE) return
        if (window?.id != event.windowId) refreshWindow()
        if (window?.id != event.windowId || !screenAvailable()) return

        if (settings.locked) return
        val clock = session ?: return
        val now = SystemClock.uptimeMillis()
        sawScrollEvents = true
        val wasScrolling = clock.scrollingFor(now) > 0
        clock.update(now, eligible = true, scrolled = true)
        if (!settings.foregroundMode && !wasScrolling) render(now)
      }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (running === this) {
      overlay.hide()
      refreshWindow()
    }
  }

  override fun onInterrupt() = refreshWindow()

  override fun onUnbind(intent: Intent?): Boolean {
    stop()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    stop()
    super.onDestroy()
  }

  internal fun turnOff() {
    stop()
    disableSelf()
  }

  internal fun sessionMs(now: Long, foreground: Boolean): Long? {
    val clock = session ?: return null
    clock.update(now, eligible = window != null && screenAvailable() && !settings.locked)
    return clock.totalMs(foreground)
  }

  private fun screenAvailable() = power.isInteractive && !keyguard.isKeyguardLocked

  internal fun refreshWindow() {
    if (running !== this) return
    if (!settings.enabled || !screenAvailable()) {
      endSession()
      return
    }
    val focus = focusedWindow()
    val now = SystemClock.uptimeMillis()
    if (focus == Focus.OtherApp) {
      endSession(now)
      return
    }
    val next = focus as? Focus.XWindow
    if (next?.id != window?.id) {
      overlay.hide()
      session?.update(now, eligible = false)
    }
    window = next
    if (next != null && session == null) session = ScrollClock()
    render(now)
  }

  @Suppress("DEPRECATION")
  private fun focusedWindow(): Focus {
    val visible = windows
    try {
      val focused = visible.firstOrNull { it.isFocused && it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
        ?: return Focus.Transient
      if (focused.type != AccessibilityWindowInfo.TYPE_APPLICATION) return Focus.Transient
      if (Build.VERSION.SDK_INT >= 26 && focused.isInPictureInPictureMode) return Focus.OtherApp
      val root = (if (Build.VERSION.SDK_INT >= 33) focused.getRoot(0) else focused.root) ?: return Focus.Transient
      // Read the window owner only. Feed nodes, text and scroll contents are never traversed.
      val owner = try {
        root.packageName?.toString()
      } finally {
        if (Build.VERSION.SDK_INT < 33) root.recycle()
      }
      if (owner == null || owner in shareSheetPackages) return Focus.Transient
      if (owner != X_PACKAGE) return Focus.OtherApp
      val bounds = Rect().also(focused::getBoundsInScreen)
      if (bounds.isEmpty) return Focus.Transient
      // On older Android the overlay uses the system's full app frame.
      if (Build.VERSION.SDK_INT < 30 && visible.any {
          it.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
            it.id != focused.id &&
            !bounds.contains(Rect().also(it::getBoundsInScreen))
        }) return Focus.Transient
      return Focus.XWindow(focused.id, bounds)
    } finally {
      if (Build.VERSION.SDK_INT < 33) visible.forEach { it.recycle() }
    }
  }

  private fun lockX() {
    val current = window ?: return
    if (running !== this || settings.locked || !screenAvailable() || focusedWindow() != current) return
    val now = SystemClock.uptimeMillis()
    settings.locked = true
    render(now)
    if (overlayFailed) {
      settings.locked = false
      render(now)
      overlayFailed = true
    }
  }

  private fun unlockX() {
    val current = window ?: return
    if (running !== this || !settings.locked || !screenAvailable() || focusedWindow() != current) return
    settings.locked = false
    render(SystemClock.uptimeMillis())
  }

  private fun render(now: Long) {
    handler.removeCallbacks(tick)
    val target = window
    val locked = settings.locked
    val clock = session
    clock?.update(now, eligible = target != null && !locked)
    if (target == null || clock == null) {
      overlay.hide()
      return
    }

    val foreground = settings.foregroundMode
    val scrollingFor = clock.scrollingFor(now)
    val message = if (locked) {
      "X is locked"
    } else {
      val elapsed = formatTime(clock.totalMs(foreground) / SECOND_MS)
      when {
        foreground -> "Time in X\n$elapsed"
        scrollingFor > 0 -> "Scrolling\n$elapsed"
        !sawScrollEvents -> "Waiting for scroll\n$elapsed"
        else -> "Scroll paused\n$elapsed"
      }
    }
    overlayFailed = !overlay.show(target.bounds, locked, message)
    val delay = when {
      locked || foreground -> SECOND_MS
      scrollingFor > 0 -> minOf(scrollingFor, SECOND_MS)
      else -> return
    }
    handler.postDelayed(tick, delay)
  }

  private fun endSession(now: Long = SystemClock.uptimeMillis()) {
    session?.let {
      it.update(now, eligible = false)
      settings.storeSession(it)
    }
    session = null
    window = null
    handler.removeCallbacks(tick)
    overlay.hide()
  }

  private fun stop() {
    if (running !== this) return
    endSession()
    running = null
    if (receiverRegistered) {
      unregisterReceiver(screenReceiver)
      receiverRegistered = false
    }
  }
}
