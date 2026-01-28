package app.brainbox.domain.repository

import app.brainbox.domain.model.DailyChallenge

interface GameRepository {
    fun getDailyChallenges(language: Language): Map<String, DailyChallenge>
    fun getCurrentDate(): String
}

enum class Language {
    FRENCH, ENGLISH, ARABIC
}