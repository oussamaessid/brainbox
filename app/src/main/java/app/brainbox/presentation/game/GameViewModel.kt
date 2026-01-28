package app.brainbox.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.brainbox.domain.model.Category
import app.brainbox.domain.model.GameState
import app.brainbox.domain.repository.Language
import app.brainbox.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val currentDate: String = "",
    val currentCategory: Category? = null,
    val gameState: GameState = GameState(),
    val showDialog: Boolean = false,
    val language: Language = Language.ENGLISH
)

class GameViewModel(
    private val getDailyChallengeUseCase: GetDailyChallengeUseCase,
    private val getCurrentDateUseCase: GetCurrentDateUseCase,
    private val validateGuessUseCase: ValidateGuessUseCase,
    private val calculateScoreUseCase: CalculateScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Set pour garder les jeux complétés par langue
    private val completedGames = mutableSetOf<String>()
    // Map pour garder si le jeu a été gagné ou perdu
    private val gameResults = mutableMapOf<String, Boolean>()

    fun loadDailyChallenge(language: Language, date: String? = null) {
        viewModelScope.launch {
            val currentDate = date ?: getCurrentDateUseCase()
            val gameKey = "${language.name}_$currentDate"

            // Vérifie si ce jeu a déjà été complété
            if (completedGames.contains(gameKey)) {
                // Ne charge pas le jeu s'il est déjà terminé
                return@launch
            }

            val challenge = getDailyChallengeUseCase(language, currentDate)

            challenge?.let {
                val category = it.categories.firstOrNull()
                _uiState.update { state ->
                    state.copy(
                        currentDate = currentDate,
                        currentCategory = category,
                        language = language,
                        gameState = GameState(), // Reset le jeu
                        showDialog = false
                    )
                }
            }
        }
    }

    fun isGameCompleted(language: Language, date: String? = null): Boolean {
        val currentDate = date ?: getCurrentDateUseCase()
        val gameKey = "${language.name}_$currentDate"
        return completedGames.contains(gameKey)
    }

    fun wasGameWon(language: Language, date: String? = null): Boolean {
        val currentDate = date ?: getCurrentDateUseCase()
        val gameKey = "${language.name}_$currentDate"
        return gameResults[gameKey] ?: false
    }

    fun onLetterClick(letter: String) {
        _uiState.update { state ->
            state.copy(
                gameState = state.gameState.copy(
                    userGuess = state.gameState.userGuess + letter
                )
            )
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            val currentGuess = state.gameState.userGuess
            if (currentGuess.isNotEmpty()) {
                state.copy(
                    gameState = state.gameState.copy(
                        userGuess = currentGuess.dropLast(1)
                    )
                )
            } else {
                state
            }
        }
    }

    fun onValidateGuess() {
        val currentState = _uiState.value
        val currentCategory = currentState.currentCategory ?: return
        val guess = currentState.gameState.userGuess

        val isCorrect = validateGuessUseCase(guess, currentCategory.name)

        if (isCorrect) {
            val scoreToAdd = calculateScoreUseCase(currentState.gameState.lives)
            val gameKey = "${currentState.language.name}_${currentState.currentDate}"

            // Marque le jeu comme complété et gagné
            completedGames.add(gameKey)
            gameResults[gameKey] = true // Gagné

            _uiState.update { state ->
                state.copy(
                    gameState = state.gameState.copy(
                        score = state.gameState.score + scoreToAdd,
                        isWin = true,
                        isGameOver = true
                    ),
                    showDialog = true
                )
            }
        } else {
            val newLives = currentState.gameState.lives - 1
            val currentCategory = currentState.currentCategory!!
            val newRevealedCount = minOf(
                currentState.gameState.revealedCount + 1,
                currentCategory.items.size
            )

            if (newLives > 0) {
                _uiState.update { state ->
                    state.copy(
                        gameState = state.gameState.copy(
                            lives = newLives,
                            revealedCount = newRevealedCount,
                            userGuess = ""
                        )
                    )
                }
            } else {
                val gameKey = "${currentState.language.name}_${currentState.currentDate}"

                // Marque le jeu comme complété et perdu
                completedGames.add(gameKey)
                gameResults[gameKey] = false // Perdu

                _uiState.update { state ->
                    state.copy(
                        gameState = state.gameState.copy(
                            lives = 0,
                            isWin = false,
                            isGameOver = true
                        ),
                        showDialog = true
                    )
                }
            }
        }
    }

    fun onPlayAgain() {
        _uiState.update { state ->
            state.copy(
                gameState = GameState(),
                showDialog = false
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun resetGame() {
        _uiState.update {
            GameUiState()
        }
    }
}