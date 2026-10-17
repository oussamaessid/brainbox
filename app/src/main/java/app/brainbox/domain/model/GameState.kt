package app.brainbox.domain.model

data class GameState(
    val lives: Int = MAX_LIVES,
    val score: Int = 0,
    val revealedCount: Int = 1,
    val userGuess: String = "",
    val hintsUsed: Int = 0,
    val isGameOver: Boolean = false,
    val isWin: Boolean = false
) {
    /** Nombre d'essais utilisés (mauvaises réponses + la bonne si victoire). */
    val attempts: Int get() = (MAX_LIVES - lives) + if (isWin) 1 else 0

    companion object {
        const val MAX_LIVES = 5
        const val MAX_HINTS = 1
    }
}
