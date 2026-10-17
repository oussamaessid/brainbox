package app.brainbox

import app.brainbox.domain.model.GameCalendar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GameCalendarTest {

    private val paris = TimeZone.getTimeZone("Europe/Paris")
    private val tunis = TimeZone.getTimeZone("Africa/Tunis")

    private fun millis(tz: TimeZone, y: Int, m: Int, d: Int, h: Int = 12): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, m - 1, d, h, 0)
        }.timeInMillis

    @Test
    fun `le 28 janvier 2026 est le jour 1`() {
        val calendar = GameCalendar(tunis)
        assertEquals(1, calendar.dayNumber(millis(tunis, 2026, 1, 28, 0)))
        assertEquals(1, calendar.dayNumber(millis(tunis, 2026, 1, 28, 23)))
        assertEquals(2, calendar.dayNumber(millis(tunis, 2026, 1, 29, 0)))
        assertEquals(0, calendar.dayNumber(millis(tunis, 2026, 1, 27)))
    }

    @Test
    fun `pas de jour sauté ni répété au changement d'heure`() {
        val calendar = GameCalendar(paris)
        // Passage à l'heure d'été : 29 mars 2026
        assertEquals(
            calendar.dayNumber(millis(paris, 2026, 3, 28, 0)) + 1,
            calendar.dayNumber(millis(paris, 2026, 3, 29, 0))
        )
        // Retour à l'heure d'hiver : 25 octobre 2026
        assertEquals(
            calendar.dayNumber(millis(paris, 2026, 10, 24, 0)) + 1,
            calendar.dayNumber(millis(paris, 2026, 10, 25, 0))
        )
        assertEquals(263, calendar.dayNumber(millis(paris, 2026, 10, 17)))
    }

    @Test
    fun `date au format historique avec chiffres latins`() {
        assertEquals("17/10/2026", GameCalendar(tunis).formatDate(millis(tunis, 2026, 10, 17)))
    }

    @Test
    fun `compte à rebours jusqu'à minuit`() {
        val calendar = GameCalendar(tunis)
        assertEquals(12 * 3_600_000L, calendar.millisUntilNextDay(millis(tunis, 2026, 10, 17, 12)))
    }
}
