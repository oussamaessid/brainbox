package app.brainbox.presentation.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.domain.repository.Language

@Composable
fun GameHeader(
    date: String,
    score: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
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

        // Date Display
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Score Display
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$score",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun WordsCard(
    items: List<String>,
    revealedCount: Int,
    language: Language = Language.ENGLISH  // 🔥 Ajout du paramètre language
) {
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
            items.forEachIndexed { index, item ->
                AnimatedWordCard(
                    word = item,
                    index = index,
                    isRevealed = index < revealedCount,
                    language = language  // 🔥 Passer le paramètre language
                )
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedWordCard(
    word: String,
    index: Int,
    isRevealed: Boolean,
    language: Language = Language.ENGLISH  // 🔥 Ajout du paramètre language
) {
    // 🔥 Détecter si c'est de l'arabe
    val isArabic = language == Language.ARABIC

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
                verticalAlignment = Alignment.CenterVertically,
                // 🔥 IMPORTANT: Aligner à droite pour l'arabe
                horizontalArrangement = if (isArabic)
                    Arrangement.End
                else
                    Arrangement.Start
            ) {
                // 🔥 IMPORTANT: Inverser l'ordre pour l'arabe
                if (isArabic) {
                    // ARABE: Texte d'abord (à droite), puis point (à gauche)
                    Text(
                        text = word,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = LocalTextStyle.current.copy(
                            textDirection = TextDirection.Rtl  // 🔥 Direction RTL
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
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
                } else {
                    // FRANÇAIS/ANGLAIS: Point d'abord, puis texte
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
}

@Composable
fun AnswerInput(
    userGuess: String,
    lives: Int,
    language: Language
) {
    val livesText = when (language) {
        Language.FRENCH -> "Votre réponse"
        Language.ENGLISH -> "Your answer"
        Language.ARABIC -> "إجابتك"
    }

    val livesLabel = when (language) {
        Language.FRENCH -> "VIES"
        Language.ENGLISH -> "LIVES"
        Language.ARABIC -> "الحياة"
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = livesText,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$livesLabel: ",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                    repeat(lives) {
                        Text(
                            text = "❤️",
                            fontSize = 12.sp
                        )
                    }
                }
            }

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

@Composable
fun ResultDialog(
    isWin: Boolean,
    categoryName: String,
    score: Int,
    language: Language,
    onPlayAgain: () -> Unit
) {
    val (congrats, tooBad, categoryWas, playAgain) = when (language) {
        Language.FRENCH -> listOf("BRAVO !", "DOMMAGE !", "La catégorie était :", "REJOUER")
        Language.ENGLISH -> listOf("CONGRATS!", "TOO BAD!", "The category was:", "PLAY AGAIN")
        Language.ARABIC -> listOf("!أحسنت", "!للأسف", ":الفئة كانت", "العب مرة أخرى")
    }

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWin) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
            ) {
                Text(
                    "$playAgain ${if (isWin) "🚀" else "💪"}",
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
                    text = if (isWin) congrats else tooBad,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = if (isWin) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = categoryWas,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = categoryName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (isWin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "+$score points !",
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