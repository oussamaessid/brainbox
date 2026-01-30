package app.brainbox.utils

import android.content.Context
import android.content.SharedPreferences
import app.brainbox.domain.repository.Language

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "brainbox_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"

        // Keys pour les scores
        private const val KEY_SCORE_PREFIX = "score_"

        // Keys pour les résultats des jeux
        private const val KEY_GAME_RESULT_PREFIX = "game_result_"
        private const val KEY_GAME_COMPLETED_PREFIX = "game_completed_"
    }

    /**
     * Check if this is the first time the app is launched
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Mark that the app has been launched (tutorial has been shown)
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * Reset first launch flag (useful for testing)
     */
    fun resetFirstLaunch() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }

    // ==================== SCORE MANAGEMENT ====================

    /**
     * Get total score for a specific language
     */
    fun getScore(language: Language): Int {
        val key = "${KEY_SCORE_PREFIX}${language.name}"
        return prefs.getInt(key, 0)
    }

    /**
     * Save total score for a specific language
     */
    fun saveScore(language: Language, score: Int) {
        val key = "${KEY_SCORE_PREFIX}${language.name}"
        prefs.edit().putInt(key, score).apply()
        println("💾 Score sauvegardé: $language = $score points")
    }

    /**
     * Add points to the existing score for a language
     */
    fun addScore(language: Language, pointsToAdd: Int) {
        val currentScore = getScore(language)
        val newScore = currentScore + pointsToAdd
        saveScore(language, newScore)
        println("➕ Score ajouté: $language: $currentScore + $pointsToAdd = $newScore")
    }

    /**
     * Reset score for a specific language
     */
    fun resetScore(language: Language) {
        saveScore(language, 0)
    }

    /**
     * Get scores for all languages
     */
    fun getAllScores(): Map<Language, Int> {
        return mapOf(
            Language.FRENCH to getScore(Language.FRENCH),
            Language.ENGLISH to getScore(Language.ENGLISH),
            Language.ARABIC to getScore(Language.ARABIC)
        )
    }

    // ==================== GAME RESULT MANAGEMENT ====================

    /**
     * Save game result for a specific date and language
     */
    fun saveGameResult(language: Language, date: String, isWin: Boolean, score: Int) {
        val gameKey = "${language.name}_$date"

        // Sauvegarder si le jeu est complété
        prefs.edit()
            .putBoolean("${KEY_GAME_COMPLETED_PREFIX}$gameKey", true)
            .putBoolean("${KEY_GAME_RESULT_PREFIX}$gameKey", isWin)
            .apply()

        // Ajouter le score au total si victoire
        if (isWin) {
            addScore(language, score)
        }

        println("💾 Résultat sauvegardé: $gameKey = ${if (isWin) "GAGNÉ" else "PERDU"} (${if (isWin) "+$score pts" else "0 pts"})")
    }

    /**
     * Check if game is completed for a specific date and language
     */
    fun isGameCompleted(language: Language, date: String): Boolean {
        val gameKey = "${language.name}_$date"
        val key = "${KEY_GAME_COMPLETED_PREFIX}$gameKey"
        val isCompleted = prefs.getBoolean(key, false)
        println("🔍 isGameCompleted($language, $date) = $isCompleted")
        return isCompleted
    }

    /**
     * Check if game was won for a specific date and language
     */
    fun wasGameWon(language: Language, date: String): Boolean {
        val gameKey = "${language.name}_$date"
        val key = "${KEY_GAME_RESULT_PREFIX}$gameKey"
        val wasWon = prefs.getBoolean(key, false)
        println("🏆 wasGameWon($language, $date) = $wasWon")
        return wasWon
    }

    /**
     * Get game result for a specific date and language
     * Returns: null if not played, true if won, false if lost
     */
    fun getGameResult(language: Language, date: String): Boolean? {
        if (!isGameCompleted(language, date)) {
            return null
        }
        return wasGameWon(language, date)
    }

    /**
     * Clear all game results (useful for testing)
     */
    fun clearAllGameResults() {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_GAME_COMPLETED_PREFIX) ||
                key.startsWith(KEY_GAME_RESULT_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
        println("🗑️ Tous les résultats de jeux ont été effacés")
    }

    /**
     * Clear all scores (useful for testing)
     */
    fun clearAllScores() {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_SCORE_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
        println("🗑️ Tous les scores ont été effacés")
    }

    /**
     * Clear everything (useful for testing)
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        println("🗑️ Toutes les données ont été effacées")
    }
}