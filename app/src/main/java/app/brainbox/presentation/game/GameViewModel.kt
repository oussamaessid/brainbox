package app.brainbox.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.brainbox.data.player.PlayerRepository
import app.brainbox.domain.model.Category
import app.brainbox.domain.model.GameState
import app.brainbox.domain.model.GuessResult
import app.brainbox.domain.model.PlayerStats
import app.brainbox.domain.repository.GameRepository
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
    val challengeNumber: Int = 0,
    val currentCategory: Category? = null,
    val gameState: GameState = GameState(),
    val showDialog: Boolean = false,
    val language: Language = Language.ENGLISH,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Réponse presque juste : on prévient le joueur sans lui retirer de vie. */
    val showCloseGuess: Boolean = false
)

class GameViewModel(
    private val repository: GameRepository,
    private val getDailyChallengeUseCase: GetDailyChallengeUseCase,
    private val validateGuessUseCase: ValidateGuessUseCase,
    private val calculateScoreUseCase: CalculateScoreUseCase,
    private val buildShareTextUseCase: BuildShareTextUseCase,
    private val preferencesManager: PreferencesManager,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun loadDailyChallenge(language: Language) {
        preferencesManager.setLastLanguage(language)
        viewModelScope.launch {
            val isLanguageChange = _uiState.value.language != language

            // Si changement de langue → vider currentCategory immédiatement
            // pour éviter le flash de l'ancien contenu avant le loading
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    language = language,
                    currentCategory = if (isLanguageChange) null else it.currentCategory
                )
            }

            val challenge = try {
                getDailyChallengeUseCase(language)
            } catch (e: Exception) {
                null
            }

            if (challenge == null) {
                val notStarted = repository.getChallengeNumber() < 1
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (notStarted) getNotStartedError(language) else getNetworkError(language)
                    )
                }
                return@launch
            }

            val totalScore = preferencesManager.getScore(language)
            val savedState = preferencesManager.getSavedGameState(language, challenge.date)
            val gameState = if (savedState != null) {
                GameState(
                    score = totalScore,
                    lives = savedState.lives,
                    revealedCount = savedState.revealedCount,
                    userGuess = savedState.userGuess,
                    hintsUsed = savedState.hintsUsed
                )
            } else {
                GameState(score = totalScore)
            }

            _uiState.update {
                it.copy(
                    currentDate = challenge.date,
                    challengeNumber = challenge.number,
                    currentCategory = challenge.category,
                    language = language,
                    gameState = gameState,
                    showDialog = false,
                    isLoading = false,
                    error = null,
                    showCloseGuess = false
                )
            }
        }
    }

    // ==================== HELPERS PUBLICS ====================

    fun isGameCompleted(language: Language): Boolean =
        preferencesManager.isGameCompleted(language, repository.getCurrentDate())

    fun wasGameWon(language: Language): Boolean =
        preferencesManager.wasGameWon(language, repository.getCurrentDate())

    fun getTotalScore(language: Language): Int = preferencesManager.getScore(language)

    fun getAllScores(): Map<Language, Int> = preferencesManager.getAllScores()

    fun getAllStats(): Map<Language, PlayerStats> = preferencesManager.getAllStats()

    fun todayNumber(): Int = repository.getChallengeNumber()

    fun millisUntilNextChallenge(): Long = repository.millisUntilNextChallenge()

    fun resetScoresAndStats() {
        preferencesManager.clearScoresAndStats()
        _uiState.update { it.copy(gameState = it.gameState.copy(score = 0)) }
    }

    /** Texte de partage du défi du jour (partie en cours d'affichage ou déjà terminée). */
    fun buildShareText(language: Language): String {
        val date = repository.getCurrentDate()
        val number = repository.getChallengeNumber()
        return buildShareTextUseCase(
            language = language,
            challengeNumber = number,
            isWin = preferencesManager.wasGameWon(language, date),
            attempts = preferencesManager.getGameAttempts(language, date),
            hintsUsed = preferencesManager.getGameHints(language, date),
            streak = preferencesManager.getStats(language).streakOn(number)
        )
    }

    // ==================== ACTIONS CLAVIER ====================

    fun onLetterClick(letter: String) {
        if (_uiState.value.gameState.isGameOver) return
        _uiState.update { state ->
            state.copy(
                gameState = state.gameState.copy(userGuess = state.gameState.userGuess + letter),
                showCloseGuess = false
            )
        }
        saveCurrentState()
    }

    fun onBackspace() {
        _uiState.update { state ->
            state.copy(
                gameState = state.gameState.copy(userGuess = state.gameState.userGuess.dropLast(1)),
                showCloseGuess = false
            )
        }
        saveCurrentState()
    }

    // ==================== VALIDATION ====================

    fun onValidateGuess() {
        val currentState = _uiState.value
        val currentCategory = currentState.currentCategory ?: return
        val gameState = currentState.gameState
        if (gameState.isGameOver || gameState.userGuess.isBlank()) return

        when (validateGuessUseCase(gameState.userGuess, currentCategory.name, currentState.language)) {
            GuessResult.CORRECT -> finishGame(isWin = true)

            GuessResult.CLOSE -> _uiState.update { it.copy(showCloseGuess = true) }

            GuessResult.WRONG -> {
                val newLives = gameState.lives - 1
                if (newLives > 0) {
                    _uiState.update { state ->
                        state.copy(
                            gameState = state.gameState.copy(
                                lives = newLives,
                                revealedCount = minOf(gameState.revealedCount + 1, currentCategory.items.size),
                                userGuess = ""
                            )
                        )
                    }
                    saveCurrentState()
                } else {
                    _uiState.update { it.copy(gameState = it.gameState.copy(lives = 0)) }
                    finishGame(isWin = false)
                }
            }
        }
    }

    private fun finishGame(isWin: Boolean) {
        val state = _uiState.value
        val category = state.currentCategory ?: return
        val finalState = state.gameState.copy(isWin = isWin, isGameOver = true)
        val scoreToAdd = if (isWin) calculateScoreUseCase(finalState.lives, finalState.hintsUsed) else 0

        preferencesManager.saveGameResult(
            language = state.language,
            date = state.currentDate,
            dayNumber = state.challengeNumber,
            isWin = isWin,
            score = scoreToAdd,
            attempts = finalState.attempts,
            hintsUsed = finalState.hintsUsed
        )
        preferencesManager.clearCurrentGameState(state.language, state.currentDate)
        if (isWin) {
            playerRepository.recordWin(state.language, state.challengeNumber, scoreToAdd)
            viewModelScope.launch { playerRepository.sync() }
        }

        _uiState.update {
            it.copy(
                gameState = finalState.copy(
                    score = finalState.score + scoreToAdd,
                    revealedCount = if (isWin) finalState.revealedCount else category.items.size
                ),
                showDialog = true,
                showCloseGuess = false
            )
        }
    }

    fun lastGameScore(): Int {
        val gameState = _uiState.value.gameState
        return if (gameState.isWin) calculateScoreUseCase(gameState.lives, gameState.hintsUsed) else 0
    }

    // ==================== INDICE (pub récompensée) ====================

    fun canUseHint(): Boolean {
        val state = _uiState.value
        val items = state.currentCategory?.items?.size ?: return false
        return !state.gameState.isGameOver &&
                state.gameState.hintsUsed < GameState.MAX_HINTS &&
                state.gameState.revealedCount < items
    }

    /** Appelé quand la pub récompensée a été regardée : révèle un mot sans perdre de vie. */
    fun onHintRewarded() {
        if (!canUseHint()) return
        _uiState.update { state ->
            state.copy(
                gameState = state.gameState.copy(
                    revealedCount = state.gameState.revealedCount + 1,
                    hintsUsed = state.gameState.hintsUsed + 1
                )
            )
        }
        saveCurrentState()
    }

    // ==================== AUTRES ACTIONS ====================

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }

    // ==================== SAUVEGARDE DE L'ÉTAT EN COURS ====================

    private fun saveCurrentState() {
        val state = _uiState.value
        if (!state.gameState.isGameOver && state.currentDate.isNotEmpty()) {
            preferencesManager.saveCurrentGameState(
                language = state.language,
                date = state.currentDate,
                state = PreferencesManager.SavedGameState(
                    lives = state.gameState.lives,
                    revealedCount = state.gameState.revealedCount,
                    userGuess = state.gameState.userGuess,
                    hintsUsed = state.gameState.hintsUsed
                )
            )
        }
    }

    private fun getNotStartedError(language: Language): String = when (language) {
        Language.ENGLISH -> "No challenge available yet. The game hasn't started."
        Language.FRENCH -> "Aucun défi disponible pour le moment. Le jeu n'a pas encore commencé."
        Language.ARABIC -> "لا يوجد تحدي متاح بعد. لم تبدأ اللعبة."
    }

    private fun getNetworkError(language: Language): String = when (language) {
        Language.ENGLISH -> "Couldn't load today's challenge. Check your internet connection and try again."
        Language.FRENCH -> "Impossible de charger le défi du jour. Vérifiez votre connexion internet et réessayez."
        Language.ARABIC -> "تعذر تحميل تحدي اليوم. تحقق من اتصالك بالإنترنت وحاول مرة أخرى."
    }

    companion object {
        fun factory(
            repository: GameRepository,
            preferencesManager: PreferencesManager,
            playerRepository: PlayerRepository
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    GameViewModel(
                        repository = repository,
                        getDailyChallengeUseCase = GetDailyChallengeUseCase(repository),
                        validateGuessUseCase = ValidateGuessUseCase(),
                        calculateScoreUseCase = CalculateScoreUseCase(),
                        buildShareTextUseCase = BuildShareTextUseCase(),
                        preferencesManager = preferencesManager,
                        playerRepository = playerRepository
                    )
                }
            }
    }
}
