package app.brainbox.domain.model

import app.brainbox.domain.repository.Language
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

data class PlayerProfile(val id: String, val name: String, val avatar: Int)

data class LeaderboardEntry(
    val uid: String,
    val name: String,
    val avatar: Int,
    val score: Int,
    val wins: Int,
    val rank: Int
)

data class LeaderboardPage(
    val entries: List<LeaderboardEntry>,
    /** Ma ligne, si je suis classé (même hors du top affiché). */
    val me: LeaderboardEntry?,
    /** true si la page vient du cache (hors-ligne). */
    val cached: Boolean = false
)

/** Mêmes identifiants que dans Equal To : 0–7 avatars classiques, 8–55 avatars Ludo. */
object PlayerAvatars {
    const val COUNT = 56
    fun isValid(id: Int) = id in 0 until COUNT
}

/** Classement par langue, ou toutes langues confondues (scope = null). */
fun Language?.boardCode(): String = when (this) {
    Language.FRENCH -> "FR"
    Language.ENGLISH -> "EN"
    Language.ARABIC -> "AR"
    null -> "ALL"
}

enum class LeaderboardPeriod(val key: String) {
    DAY("day"), WEEK("week"), MONTH("month");

    /** Identifiant de période à partir du numéro de défi : 2026-10-17, 2026-W42 (ISO) ou 2026-10. */
    fun id(dayNumber: Int): String {
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = GameCalendar.epochDayOf(dayNumber) * 86_400_000L
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return when (this) {
            // Locale.US : chiffres latins même sur un téléphone en arabe (ids Firestore).
            DAY -> String.format(Locale.US, "%04d-%02d-%02d", year, month, calendar.get(Calendar.DAY_OF_MONTH))
            WEEK -> String.format(Locale.US, "%04d-W%02d", calendar.weekYear, calendar.get(Calendar.WEEK_OF_YEAR))
            MONTH -> String.format(Locale.US, "%04d-%02d", year, month)
        }
    }

    fun boardId(scope: Language?, dayNumber: Int) = "${key}_${scope.boardCode()}_${id(dayNumber)}"
}
