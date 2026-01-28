package app.brainbox.domain.usecase

class CalculateScoreUseCase {
    operator fun invoke(lives: Int): Int {
        return lives * 20
    }
}