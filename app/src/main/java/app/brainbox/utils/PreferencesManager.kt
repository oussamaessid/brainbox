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

        // Keys pour l'état en cours de la partie
        private const val KEY_GAME_STATE_PREFIX = "game_state_"
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun resetFirstLaunch() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }


    fun getScore(language: Language): Int {
        val key = "${KEY_SCORE_PREFIX}${language.name}"
        return prefs.getInt(key, 0)
    }

    fun saveScore(language: Language, score: Int) {
        val key = "${KEY_SCORE_PREFIX}${language.name}"
        prefs.edit().putInt(key, score).apply()
        println("💾 Score sauvegardé: $language = $score points")
    }

    fun addScore(language: Language, pointsToAdd: Int) {
        val currentScore = getScore(language)
        val newScore = currentScore + pointsToAdd
        saveScore(language, newScore)
        println("➕ Score ajouté: $language: $currentScore + $pointsToAdd = $newScore")
    }

    fun resetScore(language: Language) {
        saveScore(language, 0)
    }

    fun getAllScores(): Map<Language, Int> {
        return mapOf(
            Language.FRENCH to getScore(Language.FRENCH),
            Language.ENGLISH to getScore(Language.ENGLISH),
            Language.ARABIC to getScore(Language.ARABIC)
        )
    }

    // ==================== GAME RESULT MANAGEMENT ====================

    fun saveGameResult(language: Language, date: String, isWin: Boolean, score: Int) {
        val gameKey = "${language.name}_$date"

        prefs.edit()
            .putBoolean("${KEY_GAME_COMPLETED_PREFIX}$gameKey", true)
            .putBoolean("${KEY_GAME_RESULT_PREFIX}$gameKey", isWin)
            .apply()

        if (isWin) {
            addScore(language, score)
        }

        println("💾 Résultat sauvegardé: $gameKey = ${if (isWin) "GAGNÉ" else "PERDU"} (${if (isWin) "+$score pts" else "0 pts"})")
    }

    fun isGameCompleted(language: Language, date: String): Boolean {
        val gameKey = "${language.name}_$date"
        val key = "${KEY_GAME_COMPLETED_PREFIX}$gameKey"
        val isCompleted = prefs.getBoolean(key, false)
        println("🔍 isGameCompleted($language, $date) = $isCompleted")
        return isCompleted
    }

    fun wasGameWon(language: Language, date: String): Boolean {
        val gameKey = "${language.name}_$date"
        val key = "${KEY_GAME_RESULT_PREFIX}$gameKey"
        val wasWon = prefs.getBoolean(key, false)
        println("🏆 wasGameWon($language, $date) = $wasWon")
        return wasWon
    }

    fun getGameResult(language: Language, date: String): Boolean? {
        if (!isGameCompleted(language, date)) {
            return null
        }
        return wasGameWon(language, date)
    }

    // ==================== IN-PROGRESS GAME STATE ====================

    /**
     * Modèle représentant l'état sauvegardé d'une partie en cours
     */
    data class SavedGameState(
        val lives: Int,
        val revealedCount: Int,
        val userGuess: String
    )

    /**
     * Sauvegarde l'état en cours de la partie (appelé à chaque action du joueur)
     */
    fun saveCurrentGameState(
        language: Language,
        date: String,
        lives: Int,
        revealedCount: Int,
        userGuess: String
    ) {
        val key = "${KEY_GAME_STATE_PREFIX}${language.name}_$date"
        prefs.edit()
            .putInt("${key}_lives", lives)
            .putInt("${key}_revealedCount", revealedCount)
            .putString("${key}_userGuess", userGuess)
            .apply()
        println("💾 État sauvegardé: $language/$date → lives=$lives, revealed=$revealedCount, guess='$userGuess'")
    }

    /**
     * Récupère l'état sauvegardé d'une partie en cours.
     * Retourne null si aucun état n'existe (nouvelle partie).
     */
    fun getSavedGameState(language: Language, date: String): SavedGameState? {
        val key = "${KEY_GAME_STATE_PREFIX}${language.name}_$date"
        val lives = prefs.getInt("${key}_lives", -1)

        // -1 signifie qu'aucun état n'a été sauvegardé
        if (lives == -1) {
            println("ℹ️ Aucun état sauvegardé pour $language/$date → nouvelle partie")
            return null
        }

        val state = SavedGameState(
            lives = lives,
            revealedCount = prefs.getInt("${key}_revealedCount", 0),
            userGuess = prefs.getString("${key}_userGuess", "") ?: ""
        )
        println("✅ État restauré: $language/$date → $state")
        return state
    }

    /**
     * Efface l'état en cours (appelé quand la partie est terminée : victoire ou défaite)
     */
    fun clearCurrentGameState(language: Language, date: String) {
        val key = "${KEY_GAME_STATE_PREFIX}${language.name}_$date"
        prefs.edit()
            .remove("${key}_lives")
            .remove("${key}_revealedCount")
            .remove("${key}_userGuess")
            .apply()
        println("🗑️ État effacé: $language/$date")
    }

    // ==================== CLEAR / RESET ====================

    fun clearAllGameResults() {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_GAME_COMPLETED_PREFIX) ||
                key.startsWith(KEY_GAME_RESULT_PREFIX)
            ) {
                editor.remove(key)
            }
        }
        editor.apply()
        println("🗑️ Tous les résultats de jeux ont été effacés")
    }

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

    fun clearAll() {
        prefs.edit().clear().apply()
        println("🗑️ Toutes les données ont été effacées")
    }
}