package app.xmanager.scrolltimer

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

  private data class XWindow(val id: Int, val bounds: Rect)

  private val settings by lazy { ScrollTimerSettings(this) }
  private val overlay by lazy { TimerOverlay(this, ::lockX, ::unlockX) }
  private val power by lazy { getSystemService(PowerManager::class.java)!! }
  private val keyguard by lazy { getSystemService(KeyguardManager::class.java)!! }
  private val handler = Handler(Looper.getMainLooper())
  private val tick = Runnable { refreshWindow() }
  private var receiverRegistered = false
  private var window: XWindow? = null
  private var clock = ScrollClock(false, 0)
  private var lastSavedAt = 0L

  internal var sawScrollEvents = false
    private set
  internal var overlayFailed = false
    private set

  private val screenReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == Intent.ACTION_SCREEN_OFF) pause() else refreshWindow()
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    if (!settings.enabled) {
      disableSelf()
      return
    }
    running = this
    clock = readClock()
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

        val now = SystemClock.uptimeMillis()
        if (settings.locked) return
        sawScrollEvents = true
        if (!clock.foregroundMode) {
          val wasCounting = clock.countingFor(now) > 0
          clock.update(now, eligible = true, scrolled = true)
          if (!wasCounting) render(now)
        }
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

  internal fun preferencesChanged(reset: Boolean = false) {
    val now = SystemClock.uptimeMillis()
    if (reset || settings.foregroundMode != clock.foregroundMode) {
      clock.update(now, eligible = false)
      if (!reset) settings.storeTotal(clock)
      clock = readClock()
    }
    refreshWindow()
  }

  internal fun totalMs(now: Long): Long {
    clock.update(now, eligible = window != null && screenAvailable() && !settings.locked)
    return clock.totalMs
  }

  private fun readClock() = ScrollClock(settings.foregroundMode, settings.totalMs(settings.foregroundMode))

  private fun screenAvailable() = power.isInteractive && !keyguard.isKeyguardLocked

  private fun refreshWindow() {
    if (running !== this || !settings.enabled || !screenAvailable()) {
      pause()
      return
    }
    val next = focusedXWindow()
    val now = SystemClock.uptimeMillis()
    if (next?.id != window?.id) overlay.hide()
    if (next?.id != window?.id && !clock.foregroundMode) clock.update(now, eligible = false)
    window = next
    render(now)
  }

  @Suppress("DEPRECATION")
  private fun focusedXWindow(): XWindow? {
    val visible = windows
    try {
      val focused = visible.firstOrNull { it.isFocused && it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
        ?: return null
      if (focused.type != AccessibilityWindowInfo.TYPE_APPLICATION) return null
      if (Build.VERSION.SDK_INT >= 26 && focused.isInPictureInPictureMode) return null
      val root = (if (Build.VERSION.SDK_INT >= 33) focused.getRoot(0) else focused.root) ?: return null
      // Read the window owner only. Feed nodes, text and scroll contents are never traversed.
      val isX = try {
        root.packageName?.toString() == X_PACKAGE
      } finally {
        if (Build.VERSION.SDK_INT < 33) root.recycle()
      }
      if (!isX) return null
      val bounds = Rect().also(focused::getBoundsInScreen)
      if (bounds.isEmpty) return null
      // On older Android the overlay uses the system's full app frame.
      if (Build.VERSION.SDK_INT < 30 && visible.any {
          it.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
            it.id != focused.id &&
            !bounds.contains(Rect().also(it::getBoundsInScreen))
        }) return null
      return XWindow(focused.id, bounds)
    } finally {
      if (Build.VERSION.SDK_INT < 33) visible.forEach { it.recycle() }
    }
  }

  private fun lockX() {
    val current = window ?: return
    if (running !== this || settings.locked || !screenAvailable() || focusedXWindow() != current) return
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
    if (running !== this || !settings.locked || !screenAvailable() || focusedXWindow() != current) return
    settings.locked = false
    render(SystemClock.uptimeMillis())
  }

  private fun render(now: Long) {
    handler.removeCallbacks(tick)
    val target = window
    val locked = settings.locked
    clock.update(now, eligible = target != null && !locked)
    val countingFor = clock.countingFor(now)
    if (countingFor == 0L || now - lastSavedAt >= 15 * SECOND_MS) {
      settings.storeTotal(clock)
      lastSavedAt = now
    }
    if (target == null) {
      overlay.hide()
      return
    }

    val message = if (locked) {
      "X is locked"
    } else {
      val elapsed = formatTime(clock.totalMs / SECOND_MS)
      when {
        clock.foregroundMode -> "Time in X\n$elapsed"
        countingFor > 0 -> "Scrolling\n$elapsed"
        !sawScrollEvents -> "Waiting for scroll\n$elapsed"
        else -> "Scroll paused\n$elapsed"
      }
    }
    overlayFailed = !overlay.show(target.bounds, locked, message)
    val delay = when {
      locked -> SECOND_MS
      countingFor > 0 -> minOf(countingFor, SECOND_MS)
      else -> return
    }
    handler.postDelayed(tick, delay)
  }

  private fun pause() {
    if (running !== this) return
    window = null
    render(SystemClock.uptimeMillis())
  }

  private fun stop() {
    if (running !== this) return
    pause()
    running = null
    if (receiverRegistered) {
      unregisterReceiver(screenReceiver)
      receiverRegistered = false
    }
  }
}
