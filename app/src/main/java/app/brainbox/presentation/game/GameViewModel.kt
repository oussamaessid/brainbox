package app.brainbox.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.brainbox.domain.model.Category
import app.brainbox.domain.model.GameState
import app.brainbox.domain.repository.Language
import app.brainbox.domain.usecase.*
import app.brainbox.utils.PreferencesManager
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
    private val calculateScoreUseCase: CalculateScoreUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun loadDailyChallenge(language: Language, date: String? = null) {
        viewModelScope.launch {
            try {
                println("═══════════════════════════════════════")
                println("🎮 Début loadDailyChallenge")
                println("   Langue: $language")
                println("   Date demandée: ${date ?: "aujourd'hui"}")

                _uiState.update { it.copy(isLoading = true, error = null) }

                val currentDate = date ?: getCurrentDateUseCase()
                println("📅 Date utilisée: $currentDate")

                // getDailyChallengeUseCase est maintenant suspend
                val challenge = getDailyChallengeUseCase(language, currentDate)

                if (challenge != null) {
                    val category = challenge.categories.firstOrNull()

                    if (category != null) {
                        println("✅ Challenge trouvé!")
                        println("   Catégorie: ${category.name}")
                        println("   Items: ${category.items.joinToString(", ")}")

                        // Charger le score total
                        val totalScore = preferencesManager.getScore(language)
                        println("📊 Score total chargé pour $language: $totalScore")

                        _uiState.update { state ->
                            state.copy(
                                currentDate = currentDate,
                                currentCategory = category,
                                language = language,
                                gameState = GameState(score = totalScore),
                                showDialog = false,
                                isLoading = false,
                                error = null
                            )
                        }
                    } else {
                        println("❌ Challenge trouvé mais pas de catégorie!")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = getNoDataError(language)
                            )
                        }
                    }
                } else {
                    println("❌ AUCUN challenge pour $currentDate")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = getNoChallengeError(language, currentDate)
                        )
                    }
                }
                println("═══════════════════════════════════════")
            } catch (e: Exception) {
                println("═══════════════════════════════════════")
                println("💥 Erreur loadDailyChallenge:")
                println("   Message: ${e.message}")
                println("   Type: ${e.javaClass.simpleName}")
                e.printStackTrace()
                println("═══════════════════════════════════════")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = getLoadError(language, e.message)
                    )
                }
            }
        }
    }

    fun isGameCompleted(language: Language, date: String? = null): Boolean {
        val currentDate = date ?: getCurrentDateUseCase()
        return preferencesManager.isGameCompleted(language, currentDate)
    }

    fun wasGameWon(language: Language, date: String? = null): Boolean {
        val currentDate = date ?: getCurrentDateUseCase()
        return preferencesManager.wasGameWon(language, currentDate)
    }

    fun getTotalScore(language: Language): Int {
        return preferencesManager.getScore(language)
    }

    fun getAllScores(): Map<Language, Int> {
        return preferencesManager.getAllScores()
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

        println("🔍 Validation: '$guess' vs '${currentCategory.name}'")
        val isCorrect = validateGuessUseCase(
            guess.trim(),
            currentCategory.name.trim()
        )
        println("   Résultat: ${if (isCorrect) "✅ Correct" else "❌ Incorrect"}")

        if (isCorrect) {
            val scoreToAdd = calculateScoreUseCase(currentState.gameState.lives)
            val newTotalScore = currentState.gameState.score + scoreToAdd

            println("🎉 VICTOIRE! Score de la partie: +$scoreToAdd")
            println("📊 Score total: ${currentState.gameState.score} + $scoreToAdd = $newTotalScore")

            // Sauvegarder le résultat et le score
            preferencesManager.saveGameResult(
                language = currentState.language,
                date = currentState.currentDate,
                isWin = true,
                score = scoreToAdd
            )

            _uiState.update { state ->
                state.copy(
                    gameState = state.gameState.copy(
                        score = newTotalScore,
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

            println("💔 Vie perdue! Vies restantes: $newLives")
            println("   Items révélés: $newRevealedCount/${currentCategory.items.size}")

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
                println("💀 GAME OVER! Toutes les vies perdues")

                preferencesManager.saveGameResult(
                    language = currentState.language,
                    date = currentState.currentDate,
                    isWin = false,
                    score = 0
                )

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
        println("🔄 Play Again - Rechargement du challenge")
        loadDailyChallenge(_uiState.value.language, _uiState.value.currentDate)
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    fun resetGame() {
        println("🔄 Reset Game - Nettoyage UI uniquement")
        _uiState.update { GameUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Messages d'erreur selon la langue
    private fun getNoChallengeError(language: Language, date: String): String {
        return when (language) {
            Language.ENGLISH -> "No challenge available for $date. The game may not have started yet."
            Language.FRENCH -> "Aucun défi disponible pour le $date. Le jeu n'a peut-être pas encore commencé."
            Language.ARABIC -> "لا يوجد تحدي متاح لـ $date. ربما لم تبدأ اللعبة بعد."
        }
    }

    private fun getNoDataError(language: Language): String {
        return when (language) {
            Language.ENGLISH -> "Challenge data is incomplete. Please try again."
            Language.FRENCH -> "Les données du défi sont incomplètes. Veuillez réessayer."
            Language.ARABIC -> "بيانات التحدي غير مكتملة. يرجى المحاولة مرة أخرى."
        }
    }

    private fun getLoadError(language: Language, message: String?): String {
        val baseMessage = when (language) {
            Language.ENGLISH -> "Error loading game"
            Language.FRENCH -> "Erreur de chargement"
            Language.ARABIC -> "خطأ في التحميل"
        }
        return if (message != null) "$baseMessage: $message" else baseMessage
    }
}