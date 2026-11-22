package app.xmanager.scrolltimer

import android.content.Context

internal class ScrollTimerSettings(context: Context) {
  private val prefs = context.getSharedPreferences("scroll_timer", Context.MODE_PRIVATE)
  var enabled: Boolean
    get() = prefs.getBoolean("enabled", false)
    set(value) = prefs.edit().putBoolean("enabled", value).apply()

  var foregroundMode: Boolean
    get() = prefs.getBoolean("foreground_mode", false)
    set(value) = prefs.edit().putBoolean("foreground_mode", value).apply()

  var locked: Boolean
    get() = prefs.getBoolean("locked", false)
    set(value) = prefs.edit().putBoolean("locked", value).apply()

  fun lastSessionMs(foreground: Boolean) =
    prefs.getLong(if (foreground) "last_session_foreground_ms" else "last_session_scroll_ms", 0)

  fun storeSession(clock: ScrollClock) {
    prefs.edit()
      .putLong("last_session_scroll_ms", clock.scrollMs)
      .putLong("last_session_foreground_ms", clock.foregroundMs)
      .apply()
  }
}
