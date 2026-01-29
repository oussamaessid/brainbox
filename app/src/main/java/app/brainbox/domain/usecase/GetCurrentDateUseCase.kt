package app.brainbox.domain.usecase

import app.brainbox.domain.repository.GameRepository

class GetCurrentDateUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(): String {
        val currentDate = repository.getCurrentDate()
        println("📅 GetCurrentDateUseCase: $currentDate")
        return currentDate
    }
}