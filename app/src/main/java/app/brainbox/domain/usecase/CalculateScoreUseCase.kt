package app.brainbox.domain.usecase

class CalculateScoreUseCase {
    operator fun invoke(lives: Int, hintsUsed: Int = 0): Int {
        return (lives * 20 - hintsUsed * 10).coerceAtLeast(10)
    }
}
