package app.xmanager.scrolltimer

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollClockTest {
  @Test
  fun readingStopsScrollTimeWithinTheSession() {
    val session = ScrollClock()
    session.update(2_000, eligible = true, scrolled = true)
    session.update(2_700, eligible = true, scrolled = true)
    session.update(7_000, eligible = true)

    assertEquals(1_900L, session.totalMs(foreground = false))
    assertEquals(5_000L, session.totalMs(foreground = true))
    assertEquals(0L, session.scrollingFor(7_000))
  }

  @Test
  fun transientPauseKeepsTimeAndStopsCountingUntilXReturns() {
    val session = ScrollClock()
    session.update(2_000, eligible = true, scrolled = true)
    session.update(2_800, eligible = false)
    session.update(8_000, eligible = true)

    assertEquals(800L, session.totalMs(foreground = false))
    assertEquals(800L, session.totalMs(foreground = true))
    assertEquals(0L, session.scrollingFor(8_000))

    session.update(8_500, eligible = true, scrolled = true)
    session.update(9_000, eligible = false)
    assertEquals(1_300L, session.totalMs(foreground = false))
    assertEquals(1_800L, session.totalMs(foreground = true))
  }

  @Test
  fun eachNewSessionStartsAtZero() {
    val previous = ScrollClock()
    previous.update(2_000, eligible = true, scrolled = true)
    previous.update(3_000, eligible = false)

    val next = ScrollClock()
    next.update(10_000, eligible = true)
    assertEquals(1_000L, previous.totalMs(foreground = false))
    assertEquals(0L, next.totalMs(foreground = false))
    assertEquals(0L, next.totalMs(foreground = true))
  }
}
