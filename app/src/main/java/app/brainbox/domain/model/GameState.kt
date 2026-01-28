package app.brainbox.domain.model

data class GameState(
    val currentCategoryIndex: Int = 0,
    val lives: Int = 5,
    val score: Int = 0,
    val revealedCount: Int = 1,
    val userGuess: String = "",
    val isGameOver: Boolean = false,
    val isWin: Boolean = false
)