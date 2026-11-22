package app.xmanager.scrolltimer

import java.util.Locale

private const val SCROLL_IDLE_MS = 1_200L

internal class ScrollClock(
  val foregroundMode: Boolean,
  totalMs: Long,
) {
  var totalMs = totalMs
    private set

  private var lastUpdate = 0L
  private var eligible = false
  private var scrollUntil = 0L

  fun update(now: Long, eligible: Boolean, scrolled: Boolean = false) {
    if (this.eligible) {
      val end = if (foregroundMode) now else minOf(now, scrollUntil)
      totalMs += (end - lastUpdate).coerceAtLeast(0)
    }
    lastUpdate = now
    this.eligible = eligible
    if (!eligible) scrollUntil = 0
    if (eligible && scrolled) scrollUntil = now + SCROLL_IDLE_MS
  }

  fun countingFor(now: Long): Long = when {
    !eligible -> 0L
    foregroundMode -> Long.MAX_VALUE
    else -> (scrollUntil - now).coerceAtLeast(0)
  }
}

internal fun formatTime(seconds: Long): String {
  val hours = seconds / 3_600
  val minutes = seconds / 60 % 60
  val remainder = seconds % 60
  return if (hours == 0L) {
    String.format(Locale.ROOT, "%d:%02d", minutes, remainder)
  } else {
    String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainder)
  }
}
