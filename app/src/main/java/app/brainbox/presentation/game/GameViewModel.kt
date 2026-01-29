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
    val language: Language = Language.ENGLISH,
    val isLoading: Boolean = false,
    val error: String? = null
)

class GameViewModel(
    private val getDailyChallengeUseCase: GetDailyChallengeUseCase,
    private val getCurrentDateUseCase: GetCurrentDateUseCase,
    private val validateGuessUseCase: ValidateGuessUseCase,
    private val calculateScoreUseCase: CalculateScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val completedGames = mutableSetOf<String>()
    private val gameResults = mutableMapOf<String, Boolean>()

    fun loadDailyChallenge(language: Language, date: String? = null) {
        viewModelScope.launch {
            try {
                println("→ Début loadDailyChallenge - langue: $language")
                _uiState.update { it.copy(isLoading = true, error = null) }

                val currentDate = date ?: getCurrentDateUseCase()
                println("→ Date utilisée: $currentDate")

                // getDailyChallengeUseCase est maintenant suspend
                val challenge = getDailyChallengeUseCase(language, currentDate)

                if (challenge != null) {
                    println("→ Challenge trouvé - catégorie: ${challenge.categories.firstOrNull()?.name}")
                    val category = challenge.categories.firstOrNull()
                    _uiState.update { state ->
                        state.copy(
                            currentDate = currentDate,
                            currentCategory = category,
                            language = language,
                            gameState = GameState(),
                            showDialog = false,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    println("→ AUCUN challenge pour $currentDate")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Aucun défi trouvé pour la date $currentDate"
                        )
                    }
                }
            } catch (e: Exception) {
                println("→ Erreur loadDailyChallenge: ${e.message}")
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erreur chargement : ${e.message}"
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

        if (guess.isBlank()) return

        val isCorrect = validateGuessUseCase(guess, currentCategory.name)

        if (isCorrect) {
            val scoreToAdd = calculateScoreUseCase(currentState.gameState.lives)
            val gameKey = "${currentState.language.name}_${currentState.currentDate}"

            completedGames.add(gameKey)
            gameResults[gameKey] = true

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

                completedGames.add(gameKey)
                gameResults[gameKey] = false

                _uiState.update { state ->
                    state.copy(
                        gameState = state.gameState.copy(
                            lives = 0,
                            isWin = false,
                            isGameOver = true,
                            revealedCount = currentCategory.items.size
                        ),
                        showDialog = true
                    )
                }
            }
        }
    }

    fun onPlayAgain() {
        loadDailyChallenge(_uiState.value.language, _uiState.value.currentDate)
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun resetGame() {
        completedGames.clear()
        gameResults.clear()
        _uiState.update { GameUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}