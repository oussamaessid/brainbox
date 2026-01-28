package app.brainbox.domain.usecase

import app.brainbox.domain.model.DailyChallenge
import app.brainbox.domain.repository.GameRepository
import app.brainbox.domain.repository.Language

class GetDailyChallengeUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(language: Language, date: String): DailyChallenge? {
        val challenges = repository.getDailyChallenges(language)
        return challenges[date]
    }
}
