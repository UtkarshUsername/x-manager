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

  fun totalMs(foreground: Boolean) = prefs.getLong(totalKey(foreground), 0)

  fun storeTotal(clock: ScrollClock) {
    if (totalMs(clock.foregroundMode) != clock.totalMs) {
      prefs.edit().putLong(totalKey(clock.foregroundMode), clock.totalMs).apply()
    }
  }

  fun resetTotal(foreground: Boolean) = prefs.edit().remove(totalKey(foreground)).apply()

  private fun totalKey(foreground: Boolean) = if (foreground) "foreground_ms" else "scroll_ms"
}
