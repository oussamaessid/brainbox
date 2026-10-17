package app.brainbox

import app.brainbox.data.player.PlayerRepository
import app.brainbox.domain.model.LeaderboardPeriod
import app.brainbox.domain.repository.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.random.Random

class PlayerTest {

    // Défi n°263 = 17/10/2026 (samedi, semaine ISO 42)
    @Test
    fun `identifiants de période`() {
        assertEquals("2026-10-17", LeaderboardPeriod.DAY.id(263))
        assertEquals("2026-W42", LeaderboardPeriod.WEEK.id(263))
        assertEquals("2026-10", LeaderboardPeriod.MONTH.id(263))
        assertEquals("day_FR_2026-10-17", LeaderboardPeriod.DAY.boardId(Language.FRENCH, 263))
        assertEquals("week_ALL_2026-W42", LeaderboardPeriod.WEEK.boardId(null, 263))
    }

    @Test
    fun `semaine ISO du lundi au dimanche, y compris en fin d'année`() {
        // 19/10/2026 (lundi) = défi 265 : nouvelle semaine
        assertEquals("2026-W43", LeaderboardPeriod.WEEK.id(265))
        assertEquals("2026-W42", LeaderboardPeriod.WEEK.id(264)) // dimanche 18
        // 01/01/2027 (vendredi) appartient à la semaine 53 de 2026
        assertEquals("2026-W53", LeaderboardPeriod.WEEK.id(339))
    }

    @Test
    fun `identifiants en chiffres latins même en arabe`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("2026-10-17", LeaderboardPeriod.DAY.id(263))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `pseudos automatiques valides`() {
        val random = Random(42)
        Language.entries.forEach { language ->
            repeat(50) {
                val name = PlayerRepository.generateName(language, random)
                assertTrue(name, PlayerRepository.isValidName(name))
            }
        }
    }

    @Test
    fun `clé de pseudo insensible à la casse et aux espaces, liée à l'avatar`() {
        assertEquals(PlayerRepository.nameKey("Alice", 12), PlayerRepository.nameKey("  ALICE ", 12))
        assertNotEquals(PlayerRepository.nameKey("Alice", 12), PlayerRepository.nameKey("Alice", 13))
        assertEquals("6f757373616d61_20", PlayerRepository.nameKey("Oussama", 20))
    }
}
