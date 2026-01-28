package app.brainbox.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import app.brainbox.ads.AdManager
import app.brainbox.domain.repository.Language
import app.brainbox.presentation.components.*
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    adManager: AdManager,
    language: Language,
    date: String?,
    onBackToMenu: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val bannerAd = remember { adManager.createBannerAdView(isLanguageScreen = false) }

    val shakeOffset = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(language, date) {
        viewModel.loadDailyChallenge(language, date)
    }

    LaunchedEffect(uiState.gameState.isGameOver) {
        if (uiState.gameState.isGameOver) {
            if (uiState.gameState.isWin) {
                pulseScale.animateTo(1.1f, animationSpec = tween(100))
                pulseScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            } else {
                repeat(4) {
                    shakeOffset.animateTo(15f, animationSpec = tween(40))
                    shakeOffset.animateTo(-15f, animationSpec = tween(40))
                }
                shakeOffset.animateTo(0f, animationSpec = tween(40))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFF2E1A47)
                    ),
                    radius = 1500f
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .offset(x = shakeOffset.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            GameHeader(
                date = uiState.currentDate,
                score = uiState.gameState.score,
                onBackClick = onBackToMenu
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "✨ BRAINBOX ✨",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )

            Text(
                text = getSubtitle(language),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            uiState.currentCategory?.let { category ->
                WordsCard(
                    items = category.items,
                    revealedCount = uiState.gameState.revealedCount
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnswerInput(
                userGuess = uiState.gameState.userGuess,
                lives = uiState.gameState.lives,
                language = language
            )

            Spacer(modifier = Modifier.weight(1f))

            CustomKeyboard(
                language = language,
                onLetterClick = { viewModel.onLetterClick(it) },
                onBackspace = { viewModel.onBackspace() },
                onValidate = { viewModel.onValidateGuess() },
                validateText = getValidateText(language),
                isValidateEnabled = uiState.gameState.userGuess.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(8.dp))

            BannerAdView(bannerAd)
        }

        if (uiState.showDialog) {
            uiState.currentCategory?.let { category ->
                ResultDialog(
                    isWin = uiState.gameState.isWin,
                    categoryName = category.name,
                    score = if (uiState.gameState.isWin) uiState.gameState.lives * 20 else 0,
                    language = language,
                    onPlayAgain = {
                        viewModel.resetGame()
                        onBackToMenu()
                    }
                )
            }
        }
    }
}

fun getSubtitle(language: Language): String {
    return when (language) {
        Language.FRENCH -> "Devinez la catégorie commune !"
        Language.ENGLISH -> "Guess the common category!"
        Language.ARABIC -> "خمن الفئة المشتركة!"
    }
}

fun getValidateText(language: Language): String {
    return when (language) {
        Language.FRENCH -> "VALIDER"
        Language.ENGLISH -> "VALIDATE"
        Language.ARABIC -> "تأكيد"
    }
}