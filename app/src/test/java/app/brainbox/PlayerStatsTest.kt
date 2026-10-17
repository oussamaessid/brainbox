package app.brainbox

import app.brainbox.domain.model.PlayerStats
import app.brainbox.domain.repository.Language
import app.brainbox.domain.usecase.BuildShareTextUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStatsTest {

    @Test
    fun `la série augmente sur des jours consécutifs`() {
        val stats = PlayerStats()
            .record(dayNumber = 10, isWin = true, attempts = 2)
            .record(dayNumber = 11, isWin = true, attempts = 1)
            .record(dayNumber = 12, isWin = true, attempts = 3)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.maxStreak)
        assertEquals(listOf(1, 1, 1, 0, 0), stats.distribution)
        assertEquals(100, stats.winRate)
    }

    @Test
    fun `un jour manqué ou une défaite casse la série`() {
        val afterGap = PlayerStats()
            .record(10, true, 1)
            .record(12, true, 1)
        assertEquals(1, afterGap.currentStreak)

        val afterLoss = afterGap.record(13, false, 5)
        assertEquals(0, afterLoss.currentStreak)
        assertEquals(1, afterLoss.maxStreak)
        assertEquals(3, afterLoss.played)
        assertEquals(66, afterLoss.winRate)
    }

    @Test
    fun `la série affichée tombe à zéro si hier n'a pas été gagné`() {
        val stats = PlayerStats().record(10, true, 1)
        assertEquals(1, stats.streakOn(10))
        assertEquals(1, stats.streakOn(11))
        assertEquals(0, stats.streakOn(12))
    }

    @Test
    fun `le texte de partage ne révèle pas la réponse`() {
        val text = BuildShareTextUseCase()(
            language = Language.FRENCH,
            challengeNumber = 263,
            isWin = true,
            attempts = 3,
            hintsUsed = 1,
            streak = 4
        )
        assertTrue(text.startsWith("🧠 BrainBox #263 🇫🇷"))
        assertTrue(text.contains("🟥🟥🟩⬛⬛  3/5"))
        assertTrue(text.contains("🔥 Série : 4"))
        assertTrue(text.contains("💡 1 indice"))

        val lost = BuildShareTextUseCase()(Language.ENGLISH, 1, false, 5, 0, 0)
        assertTrue(lost.contains("🟥🟥🟥🟥🟥  X/5"))
        assertFalse(lost.contains("🔥"))
    }
}
