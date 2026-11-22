package app.xmanager.scrolltimer

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import expo.modules.kotlin.functions.Queues
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ScrollTimerModule : Module() {
  private val context: Context
    get() = requireNotNull(appContext.reactContext).applicationContext
  private val settings by lazy { ScrollTimerSettings(context) }

  override fun definition() = ModuleDefinition {
    Name("XManagerScrollTimer")

    AsyncFunction("getState") { state() }.runOnQueue(Queues.MAIN)

    AsyncFunction("setEnabled") { enabled: Boolean ->
      settings.enabled = enabled
      if (!enabled) ScrollTimerService.running?.turnOff()
      state()
    }.runOnQueue(Queues.MAIN)

    AsyncFunction("setForegroundMode") { foreground: Boolean ->
      settings.foregroundMode = foreground
      ScrollTimerService.running?.preferencesChanged()
      state()
    }.runOnQueue(Queues.MAIN)

    AsyncFunction("resetTime") {
      settings.resetTotal(settings.foregroundMode)
      ScrollTimerService.running?.preferencesChanged(reset = true)
      state()
    }.runOnQueue(Queues.MAIN)
  }

  private fun state(): Map<String, Any> {
    val service = ScrollTimerService.running
    val total = service?.totalMs(SystemClock.uptimeMillis()) ?: settings.totalMs(settings.foregroundMode)
    val component = ComponentName(context, ScrollTimerService::class.java)
    val services = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    val granted = services?.split(':')?.any { ComponentName.unflattenFromString(it) == component } == true

    return mapOf(
      "enabled" to settings.enabled,
      "granted" to granted,
      "running" to (service != null),
      "locked" to settings.locked,
      "foregroundMode" to settings.foregroundMode,
      "totalSeconds" to (total / 1_000).toDouble(),
      "sawScrollEvents" to (service?.sawScrollEvents == true),
      "overlayFailed" to (service?.overlayFailed == true),
    )
  }
}
