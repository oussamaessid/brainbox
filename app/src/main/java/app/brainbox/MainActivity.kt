package app.brainbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class LevelData(
    val category: String,
    val items: List<String>
)

data class GameTranslations(
    val title: String,
    val subtitle: String,
    val lives: String,
    val score: String,
    val yourAnswer: String,
    val newGame: String,
    val nextLevel: String,
    val retry: String,
    val congrats: String,
    val tooBad: String,
    val categoryWas: String,
    val validate: String,
    val chooseLanguage: String,
    val startGame: String
)

enum class Language {
    FRENCH, ENGLISH, ARABIC
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BrainBoxApp()
            }
        }
    }
}

@Composable
fun BrainBoxApp() {
    var selectedLanguage by remember { mutableStateOf<Language?>(null) }

    if (selectedLanguage == null) {
        LanguageSelectionScreen { language ->
            selectedLanguage = language
        }
    } else {
        GuessCategoryGame(selectedLanguage!!) {
            selectedLanguage = null
        }
    }
}

@Composable
fun LanguageSelectionScreen(onLanguageSelected: (Language) -> Unit) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✨ BRAINBOX ✨",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose Your Language",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Choisissez votre langue",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "اختر لغتك",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))


            LanguageCard(
                flag = "🇬🇧",
                language = "English",
                onClick = { onLanguageSelected(Language.ENGLISH) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LanguageCard(
                flag = "🇫🇷",
                language = "Français",
                onClick = { onLanguageSelected(Language.FRENCH) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LanguageCard(
                flag = "🇸🇦",
                language = "العربية",
                onClick = { onLanguageSelected(Language.ARABIC) }
            )
        }
    }
}

@Composable
fun LanguageCard(flag: String, language: String, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = flag,
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = language,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun GuessCategoryGame(language: Language, onBackToMenu: () -> Unit) {
    val translations = getTranslations(language)
    val gameData = getGameData(language)

    var currentLevelIndex by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(5) }
    var score by remember { mutableIntStateOf(0) }
    var revealedCount by remember { mutableIntStateOf(1) }
    var userGuess by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isWin by remember { mutableStateOf(false) }

    val currentLevel = gameData[currentLevelIndex]
    val scope = rememberCoroutineScope()

    val shakeOffset = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    fun triggerShake() {
        scope.launch {
            repeat(4) {
                shakeOffset.animateTo(15f, animationSpec = tween(40))
                shakeOffset.animateTo(-15f, animationSpec = tween(40))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(40))
        }
    }

    fun triggerPulse() {
        scope.launch {
            pulseScale.animateTo(1.1f, animationSpec = tween(100))
            pulseScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    fun checkGuess() {
        if (userGuess.trim().equals(currentLevel.category, ignoreCase = true)) {
            score += (lives * 20)
            isWin = true
            showDialog = true
            triggerPulse()
        } else {
            if (lives > 1) {
                lives--
                if (revealedCount < currentLevel.items.size) {
                    revealedCount++
                }
                userGuess = ""
                triggerShake()
            } else {
                lives = 0
                isWin = false
                showDialog = true
            }
        }
    }

    fun resetLevel() {
        if (isWin) {
            currentLevelIndex = (currentLevelIndex + 1) % gameData.size
        }
        lives = 5
        revealedCount = 1
        userGuess = ""
        showDialog = false
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onBackToMenu,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = translations.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )

            Text(
                text = translations.subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card des mots
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    currentLevel.items.forEachIndexed { index, item ->
                        AnimatedWordCard(
                            word = item,
                            index = index,
                            isRevealed = index < revealedCount
                        )
                        if (index < currentLevel.items.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.12f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${translations.yourAnswer} (${translations.lives}: $lives)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                2.dp,
                                Color(0xFFFFD93D).copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userGuess.uppercase().ifEmpty { "..." },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userGuess.isEmpty())
                                Color.White.copy(alpha = 0.3f)
                            else Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Clavier personnalisé
            CustomKeyboard(
                language = language,
                onLetterClick = { letter ->
                    userGuess += letter
                },
                onBackspace = {
                    if (userGuess.isNotEmpty()) {
                        userGuess = userGuess.dropLast(1)
                    }
                },
                onValidate = { checkGuess() },
                validateText = translations.validate,
                isValidateEnabled = userGuess.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(
                    onClick = { resetLevel() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWin) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                ) {
                    Text(
                        if (isWin) "${translations.nextLevel} 🚀" else "${translations.retry} 💪",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isWin) "🎉" else "😔",
                        fontSize = 48.sp
                    )
                    Text(
                        text = if (isWin) translations.congrats else translations.tooBad,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = if (isWin) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = translations.categoryWas,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentLevel.category,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (isWin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "+${lives * 20} points !",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD93D)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun AnimatedWordCard(word: String, index: Int, isRevealed: Boolean) {
    AnimatedVisibility(
        visible = isRevealed,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = tween(300)
                )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFFD93D), Color(0xFFFF8008))
                            ),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = word,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CustomKeyboard(
    language: Language,
    onLetterClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onValidate: () -> Unit,
    validateText: String,
    isValidateEnabled: Boolean
) {
    val rows = when (language) {
        Language.FRENCH -> listOf(
            listOf("A", "Z", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("Q", "S", "D", "F", "G", "H", "J", "K", "L", "M"),
            listOf("W", "X", "C", "V", "B", "N")
        )
        Language.ENGLISH -> listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M")
        )
        Language.ARABIC -> listOf(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح"),
            listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م"),
            listOf("ظ", "ط", "ذ", "د", "ز", "ر", "و")
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { letter ->
                    KeyButton(
                        text = letter,
                        onClick = { onLetterClick(letter) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecialKeyButton(
                text = "⌫",
                onClick = onBackspace,
                color = Color(0xFFFF6B6B)
            )

            Button(
                onClick = onValidate,
                enabled = isValidateEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    "✓ $validateText",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (isValidateEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun RowScope.KeyButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.1f)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun SpecialKeyButton(
    text: String,
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getTranslations(language: Language): GameTranslations {
    return when (language) {
        Language.FRENCH -> GameTranslations(
            title = "✨ BRAINBOX ✨",
            subtitle = "Devinez la catégorie commune !",
            lives = "VIES",
            score = "SCORE",
            yourAnswer = "Votre réponse :",
            newGame = "NOUVEAU JEU",
            nextLevel = "NIVEAU SUIVANT",
            retry = "RÉESSAYER",
            congrats = "BRAVO !",
            tooBad = "DOMMAGE !",
            categoryWas = "La catégorie était :",
            validate = "VALIDER",
            chooseLanguage = "LANGUE",
            startGame = "COMMENCER"
        )
        Language.ENGLISH -> GameTranslations(
            title = "✨ BRAINBOX ✨",
            subtitle = "Guess the common category!",
            lives = "LIVES",
            score = "SCORE",
            yourAnswer = "Your answer:",
            newGame = "NEW GAME",
            nextLevel = "NEXT LEVEL",
            retry = "TRY AGAIN",
            congrats = "CONGRATS!",
            tooBad = "TOO BAD!",
            categoryWas = "The category was:",
            validate = "VALIDATE",
            chooseLanguage = "LANGUAGE",
            startGame = "START"
        )
        Language.ARABIC -> GameTranslations(
            title = "✨ صندوق العقل ✨",
            subtitle = "خمن الفئة المشتركة!",
            lives = "الحياة",
            score = "النقاط",
            yourAnswer = ":إجابتك",
            newGame = "لعبة جديدة",
            nextLevel = "المستوى التالي",
            retry = "حاول مرة أخرى",
            congrats = "!أحسنت",
            tooBad = "!للأسف",
            categoryWas = ":الفئة كانت",
            validate = "تأكيد",
            chooseLanguage = "اللغة",
            startGame = "ابدأ"
        )
    }
}

fun getGameData(language: Language): List<LevelData> {
    return when (language) {
        Language.FRENCH -> listOf(
            LevelData("FRUITS", listOf("Pomme", "Banane", "Orange", "Raisin", "Fraise")),
            LevelData("ANIMAUX", listOf("Lion", "Éléphant", "Girafe", "Tigre", "Zèbre")),
            LevelData("PAYS", listOf("France", "Canada", "Maroc", "Japon", "Brésil")),
            LevelData("COULEURS", listOf("Rouge", "Bleu", "Vert", "Jaune", "Violet")),
            LevelData("SPORTS", listOf("Football", "Tennis", "Natation", "Basketball", "Cyclisme")),
            LevelData("METIERS", listOf("Médecin", "Professeur", "Ingénieur", "Chef", "Artiste"))
        )
        Language.ENGLISH -> listOf(
            LevelData("FRUITS", listOf("Apple", "Banana", "Orange", "Grape", "Strawberry")),
            LevelData("ANIMALS", listOf("Lion", "Elephant", "Giraffe", "Tiger", "Zebra")),
            LevelData("COUNTRIES", listOf("France", "Canada", "Morocco", "Japan", "Brazil")),
            LevelData("COLORS", listOf("Red", "Blue", "Green", "Yellow", "Purple")),
            LevelData("SPORTS", listOf("Football", "Tennis", "Swimming", "Basketball", "Cycling")),
            LevelData("JOBS", listOf("Doctor", "Teacher", "Engineer", "Chef", "Artist"))
        )
        Language.ARABIC -> listOf(
            LevelData("فواكه", listOf("تفاح", "موز", "برتقال", "عنب", "فراولة")),
            LevelData("حيوانات", listOf("أسد", "فيل", "زرافة", "نمر", "حمار وحشي")),
            LevelData("دول", listOf("فرنسا", "كندا", "المغرب", "اليابان", "البرازيل")),
            LevelData("ألوان", listOf("أحمر", "أزرق", "أخضر", "أصفر", "بنفسجي")),
            LevelData("رياضات", listOf("كرة قدم", "تنس", "سباحة", "كرة سلة", "ركوب دراجات")),
            LevelData("مهن", listOf("طبيب", "معلم", "مهندس", "طاهي", "فنان"))
        )
    }
}