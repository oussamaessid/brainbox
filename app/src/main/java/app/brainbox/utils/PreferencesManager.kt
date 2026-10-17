package app.brainbox.utils

import android.content.Context
import android.content.SharedPreferences
import app.brainbox.domain.model.GameState
import app.brainbox.domain.model.PlayerStats
import app.brainbox.domain.repository.Language

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "brainbox_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_LANGUAGE = "last_language"
        private const val KEY_NOTIFICATION_ASKED = "notification_permission_asked"
        private const val KEY_STATS_MIGRATED = "stats_migrated_v6"

        // Keys pour les scores
        private const val KEY_SCORE_PREFIX = "score_"

        // Keys pour les résultats des jeux
        private const val KEY_GAME_RESULT_PREFIX = "game_result_"
        private const val KEY_GAME_COMPLETED_PREFIX = "game_completed_"
        private const val KEY_GAME_ATTEMPTS_PREFIX = "game_attempts_"
        private const val KEY_GAME_HINTS_PREFIX = "game_hints_"

        // Keys pour l'état en cours de la partie
        private const val KEY_GAME_STATE_PREFIX = "game_state_"

        // Keys pour les statistiques
        private const val KEY_STATS_PREFIX = "stats_"
    }

    init {
        migrateStatsIfNeeded()
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getLastLanguage(): Language? =
        prefs.getString(KEY_LAST_LANGUAGE, null)?.let { runCatching { Language.valueOf(it) }.getOrNull() }

    fun setLastLanguage(language: Language) {
        prefs.edit().putString(KEY_LAST_LANGUAGE, language.name).apply()
    }

    fun wasNotificationPermissionAsked(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_ASKED, false)

    fun setNotificationPermissionAsked() {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ASKED, true).apply()
    }

    // ==================== SCORES ====================

    fun getScore(language: Language): Int {
        return prefs.getInt("${KEY_SCORE_PREFIX}${language.name}", 0)
    }

    private fun addScore(language: Language, pointsToAdd: Int) {
        prefs.edit().putInt("${KEY_SCORE_PREFIX}${language.name}", getScore(language) + pointsToAdd).apply()
    }

    fun getAllScores(): Map<Language, Int> = Language.entries.associateWith { getScore(it) }

    // ==================== GAME RESULT MANAGEMENT ====================

    fun saveGameResult(
        language: Language,
        date: String,
        dayNumber: Int,
        isWin: Boolean,
        score: Int,
        attempts: Int,
        hintsUsed: Int
    ) {
        val gameKey = "${language.name}_$date"
        if (prefs.getBoolean("${KEY_GAME_COMPLETED_PREFIX}$gameKey", false)) return

        prefs.edit()
            .putBoolean("${KEY_GAME_COMPLETED_PREFIX}$gameKey", true)
            .putBoolean("${KEY_GAME_RESULT_PREFIX}$gameKey", isWin)
            .putInt("${KEY_GAME_ATTEMPTS_PREFIX}$gameKey", attempts)
            .putInt("${KEY_GAME_HINTS_PREFIX}$gameKey", hintsUsed)
            .apply()

        if (isWin) {
            addScore(language, score)
        }
        saveStats(language, getStats(language).record(dayNumber, isWin, attempts))
    }

    fun isGameCompleted(language: Language, date: String): Boolean =
        prefs.getBoolean("${KEY_GAME_COMPLETED_PREFIX}${language.name}_$date", false)

    fun wasGameWon(language: Language, date: String): Boolean =
        prefs.getBoolean("${KEY_GAME_RESULT_PREFIX}${language.name}_$date", false)

    fun getGameAttempts(language: Language, date: String): Int =
        prefs.getInt("${KEY_GAME_ATTEMPTS_PREFIX}${language.name}_$date", GameState.MAX_LIVES)

    fun getGameHints(language: Language, date: String): Int =
        prefs.getInt("${KEY_GAME_HINTS_PREFIX}${language.name}_$date", 0)

    // ==================== STATISTICS ====================

    fun getStats(language: Language): PlayerStats {
        val key = "${KEY_STATS_PREFIX}${language.name}"
        val distribution = prefs.getString("${key}_distribution", null)
            ?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == GameState.MAX_LIVES }
            ?: List(GameState.MAX_LIVES) { 0 }
        return PlayerStats(
            played = prefs.getInt("${key}_played", 0),
            wins = prefs.getInt("${key}_wins", 0),
            currentStreak = prefs.getInt("${key}_streak", 0),
            maxStreak = prefs.getInt("${key}_maxStreak", 0),
            lastWinDay = prefs.getInt("${key}_lastWinDay", 0),
            distribution = distribution
        )
    }

    private fun saveStats(language: Language, stats: PlayerStats) {
        val key = "${KEY_STATS_PREFIX}${language.name}"
        prefs.edit()
            .putInt("${key}_played", stats.played)
            .putInt("${key}_wins", stats.wins)
            .putInt("${key}_streak", stats.currentStreak)
            .putInt("${key}_maxStreak", stats.maxStreak)
            .putInt("${key}_lastWinDay", stats.lastWinDay)
            .putString("${key}_distribution", stats.distribution.joinToString(","))
            .apply()
    }

    fun getAllStats(): Map<Language, PlayerStats> = Language.entries.associateWith { getStats(it) }

    /** Les versions ≤ 1.0.1 n'avaient pas de stats : on reconstruit parties jouées / gagnées. */
    private fun migrateStatsIfNeeded() {
        if (prefs.getBoolean(KEY_STATS_MIGRATED, false)) return
        Language.entries.forEach { language ->
            val prefix = "${KEY_GAME_COMPLETED_PREFIX}${language.name}_"
            val dates = prefs.all.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
            if (dates.isNotEmpty()) {
                val wins = dates.count { wasGameWon(language, it) }
                saveStats(language, getStats(language).copy(played = dates.size, wins = wins))
            }
        }
        prefs.edit().putBoolean(KEY_STATS_MIGRATED, true).apply()
    }

    // ==================== IN-PROGRESS GAME STATE ====================

    data class SavedGameState(
        val lives: Int,
        val revealedCount: Int,
        val userGuess: String,
        val hintsUsed: Int
    )

    /** Sauvegarde l'état en cours de la partie (appelé à chaque action du joueur). */
    fun saveCurrentGameState(language: Language, date: String, state: SavedGameState) {
        val key = "${KEY_GAME_STATE_PREFIX}${language.name}_$date"
        prefs.edit()
            .putInt("${key}_lives", state.lives)
            .putInt("${key}_revealedCount", state.revealedCount)
            .putString("${key}_userGuess", state.userGuess)
            .putInt("${key}_hintsUsed", state.hintsUsed)
            .apply()
    }

    /** Retourne null si aucun état n'existe (nouvelle partie). */
    fun getSavedGameState(language: Language, date: String): SavedGameState? {
        val key = "${KEY_GAME_STATE_PREFIX}${language.name}_$date"
        val lives = prefs.getInt("${key}_lives", -1)
        if (lives == -1) return null

        return SavedGameState(
            lives = lives,
            revealedCount = prefs.getInt("${key}_revealedCount", 1),
            userGuess = prefs.getString("${key}_userGuess", "") ?: "",
            hintsUsed = prefs.getInt("${key}_hintsUsed", 0)
        )
    }

    /** Efface l'état en cours (partie terminée : victoire ou défaite). */
    fun clearCurrentGameState(language: Language, date: String) {
        val key = "${KEY_GAME_STATE_PREFIX}${language.name}_$date"
        prefs.edit()
            .remove("${key}_lives")
            .remove("${key}_revealedCount")
            .remove("${key}_userGuess")
            .remove("${key}_hintsUsed")
            .apply()
    }

    // ==================== RESET ====================

    /** Remet à zéro scores et statistiques (les parties du jour restent jouées). */
    fun clearScoresAndStats() {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_SCORE_PREFIX) || key.startsWith(KEY_STATS_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }
}
