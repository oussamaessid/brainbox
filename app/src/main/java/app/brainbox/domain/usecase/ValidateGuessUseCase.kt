package app.brainbox.domain.usecase

class ValidateGuessUseCase {
    operator fun invoke(guess: String, correctAnswer: String): Boolean {
        return guess.trim().equals(correctAnswer, ignoreCase = true)
    }
}