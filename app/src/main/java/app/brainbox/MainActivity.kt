package app.brainbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.ads.AdManager
import app.brainbox.data.repository.GameRepositoryImpl
import app.brainbox.domain.repository.Language
import app.brainbox.domain.usecase.*
import app.brainbox.presentation.game.GameScreen
import app.brainbox.presentation.game.GameViewModel
import app.brainbox.presentation.language.LanguageSelectionScreen

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adManager = AdManager(this)

        val repository = GameRepositoryImpl()
        val getDailyChallengeUseCase = GetDailyChallengeUseCase(repository)
        val getCurrentDateUseCase = GetCurrentDateUseCase(repository)
        val validateGuessUseCase = ValidateGuessUseCase()
        val calculateScoreUseCase = CalculateScoreUseCase()

        viewModel = GameViewModel(
            getDailyChallengeUseCase = getDailyChallengeUseCase,
            getCurrentDateUseCase = getCurrentDateUseCase,
            validateGuessUseCase = validateGuessUseCase,
            calculateScoreUseCase = calculateScoreUseCase
        )

        // Charger l'annonce à l'ouverture
        adManager.loadAppOpenAd {
            // Afficher l'annonce quand elle est chargée
            adManager.showAppOpenAd(this)
        }

        setContent {
            MaterialTheme {
                BrainBoxApp(viewModel, adManager)
            }
        }
    }
}

@Composable
fun BrainBoxApp(viewModel: GameViewModel, adManager: AdManager) {
    var selectedLanguage by remember { mutableStateOf<Language?>(null) }
    var showCompletedDialog by remember { mutableStateOf(false) }
    var completedLanguage by remember { mutableStateOf<Language?>(null) }
    val uiState by viewModel.uiState.collectAsState()

    if (selectedLanguage == null) {
        LanguageSelectionScreen(
            adManager = adManager,
            onLanguageSelected = { language ->
                // Vérifie si le jeu pour cette langue est déjà terminé
                if (viewModel.isGameCompleted(language, null)) {
                    // Montre l'alerte si le jeu est déjà complété
                    completedLanguage = language
                    showCompletedDialog = true
                } else {
                    // Lance le jeu si pas encore complété
                    selectedLanguage = language
                }
            }
        )

        // Alerte pour les jeux déjà complétés
        if (showCompletedDialog && completedLanguage != null) {
            CompletedGameDialog(
                language = completedLanguage!!,
                isWin = viewModel.wasGameWon(completedLanguage!!, null),
                onDismiss = {
                    showCompletedDialog = false
                    completedLanguage = null
                }
            )
        }
    } else {
        GameScreen(
            viewModel = viewModel,
            adManager = adManager,
            language = selectedLanguage!!,
            date = null, // null pour la date actuelle
            onBackToMenu = {
                // Retour à l'écran de sélection de langue
                selectedLanguage = null
            }
        )
    }
}

@Composable
fun CompletedGameDialog(
    language: Language,
    isWin: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = if (isWin) listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF388E3C),
                                Color(0xFF1B5E20)
                            ) else listOf(
                                Color(0xFFE91E63),
                                Color(0xFFC2185B),
                                Color(0xFF880E4F)
                            ),
                            radius = 800f
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Emoji Icon
                    Text(
                        text = if (isWin) "🏆" else "💔",
                        fontSize = 72.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Title
                    Text(
                        text = if (isWin) getWinTitle(language) else getLoseTitle(language),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(3.dp)
                            .background(
                                Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(bottom = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message
                    Text(
                        text = getCompletedMessage(language, isWin),
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // OK Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = getOkText(language),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun getWinTitle(language: Language): String {
    return when (language) {
        Language.FRENCH -> "GAGNÉ"
        Language.ENGLISH -> "WON"
        Language.ARABIC -> "فوز"
    }
}

fun getLoseTitle(language: Language): String {
    return when (language) {
        Language.FRENCH -> "PERDU"
        Language.ENGLISH -> "LOST"
        Language.ARABIC -> "خسارة"
    }
}

fun getCompletedMessage(language: Language, isWin: Boolean): String {
    return when (language) {
        Language.FRENCH -> if (isWin)
            "Vous avez déjà gagné ce jeu aujourd'hui !"
        else
            "Vous avez déjà perdu ce jeu aujourd'hui. Revenez demain !"
        Language.ENGLISH -> if (isWin)
            "You already won this game today!"
        else
            "You already lost this game today. Come back tomorrow!"
        Language.ARABIC -> if (isWin)
            "!لقد فزت بهذه اللعبة اليوم بالفعل"
        else
            "!لقد خسرت هذه اللعبة اليوم. عد غداً"
    }
}

fun getOkText(language: Language): String {
    return when (language) {
        Language.FRENCH -> "OK"
        Language.ENGLISH -> "OK"
        Language.ARABIC -> "حسناً"
    }
}