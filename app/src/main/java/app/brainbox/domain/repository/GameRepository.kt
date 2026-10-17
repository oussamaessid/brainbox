package app.brainbox.domain.repository

import app.brainbox.domain.model.DailyChallenge

interface GameRepository {
    /** Défi du jour, depuis le réseau ou, hors-ligne, depuis le cache. */
    suspend fun getDailyChallenge(language: Language): DailyChallenge?
    fun getCurrentDate(): String
    fun getChallengeNumber(): Int
    fun millisUntilNextChallenge(): Long
}

enum class Language {
    FRENCH, ENGLISH, ARABIC;

    companion object {
        fun fromDeviceLocale(): Language = when (java.util.Locale.getDefault().language) {
            "fr" -> FRENCH
            "ar" -> ARABIC
            else -> ENGLISH
        }
    }
}
