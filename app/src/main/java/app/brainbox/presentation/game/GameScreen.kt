package app.brainbox.presentation.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import app.brainbox.domain.repository.Language
import app.brainbox.presentation.components.*

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
        println("🎬 GameScreen LaunchedEffect - Chargement du challenge")
        viewModel.loadDailyChallenge(language, date)
    }

    LaunchedEffect(uiState.gameState.isGameOver) {
        if (uiState.gameState.isGameOver) {
            if (uiState.gameState.isWin) {
                pulseScale.animateTo(1.1f, animationSpec = tween(100))
                pulseScale.animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
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

        // Loading indicator
        if (uiState.isLoading) {
            LoadingScreen(language = language)
        }
        // Error screen
        else if (uiState.error != null) {
            ErrorScreen(
                error = uiState.error!!,
                language = language,
                onRetry = {
                    println("🔄 Retry button clicked")
                    viewModel.loadDailyChallenge(language, date)
                },
                onBackToMenu = {
                    println("🔙 Back to menu button clicked")
                    onBackToMenu()
                }
            )
        }
        // Game content
        else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
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
                            revealedCount = uiState.gameState.revealedCount,
                            language = language  // 🔥 LIGNE AJOUTÉE POUR LE SUPPORT RTL
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
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    BannerAdView(bannerAd)
                }
            }
        }

        if (uiState.showDialog) {
            uiState.currentCategory?.let { category ->
                ResultDialog(
                    isWin = uiState.gameState.isWin,
                    categoryName = category.name,
                    score = if (uiState.gameState.isWin) uiState.gameState.lives * 20 else 0,
                    language = language,
                    onPlayAgain = {
                        println("🎮 Play Again clicked - Retour au menu")
                        viewModel.resetGame()
                        onBackToMenu()
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(language: Language) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = Color(0xFF667eea),
                    strokeWidth = 6.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = getLoadingText(language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E1A47),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = getLoadingSubtext(language),
                    fontSize = 14.sp,
                    color = Color(0xFF2E1A47).copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ErrorScreen(
    error: String,
    language: Language,
    onRetry: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFE91E63)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = getErrorTitle(language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E1A47),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = error,
                    fontSize = 16.sp,
                    color = Color(0xFF2E1A47).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Retry button
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getRetryText(language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Back to menu button
                OutlinedButton(
                    onClick = onBackToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF667eea)
                    )
                ) {
                    Text(
                        text = getBackToMenuText(language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Language strings for loading
fun getLoadingText(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Loading Game..."
        Language.FRENCH -> "Chargement du jeu..."
        Language.ARABIC -> "...تحميل اللعبة"
    }
}

fun getLoadingSubtext(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Fetching today's puzzle"
        Language.FRENCH -> "Récupération du puzzle du jour"
        Language.ARABIC -> "جلب لغز اليوم"
    }
}

// Language strings for error
fun getErrorTitle(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Oops!"
        Language.FRENCH -> "Oups !"
        Language.ARABIC -> "عذراً!"
    }
}

fun getRetryText(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Retry"
        Language.FRENCH -> "Réessayer"
        Language.ARABIC -> "إعادة المحاولة"
    }
}

fun getBackToMenuText(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Back to Menu"
        Language.FRENCH -> "Retour au menu"
        Language.ARABIC -> "العودة للقائمة"
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