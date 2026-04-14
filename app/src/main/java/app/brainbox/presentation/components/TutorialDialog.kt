package app.brainbox.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.domain.repository.Language

@Composable
fun TutorialDialog(
    language: Language = Language.ENGLISH, onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = {}, enabled = false), contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF667eea), Color(0xFF764ba2), Color(0xFF2E1A47)
                            ), radius = 1200f
                        )
                    )
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getTutorialTitle(language),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )

                        IconButton(
                            onClick = onDismiss, modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Brain emoji
                        Text(
                            text = "🧠",
                            fontSize = 64.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 16.dp)
                        )

                        // Objective section
                        SectionTitle(
                            text = getObjectiveTitle(language),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SectionText(
                            text = getObjectiveText(language),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // How to play section
                        SectionTitle(
                            text = getHowToPlayTitle(language),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        TutorialStep(
                            number = "1",
                            text = getStep1Text(language),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        TutorialStep(
                            number = "2",
                            text = getStep2Text(language),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        TutorialStep(
                            number = "3",
                            text = getStep3Text(language),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Example section
                        SectionTitle(
                            text = getExampleTitle(language),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Example 1 - Always English
                        ExampleCard(
                            clues = getExampleClues1(),
                            answer = getExampleAnswer1(),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Example 2 - Always French
                        ExampleCard(
                            clues = getExampleClues2(),
                            answer = getExampleAnswer2(),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Example 3 - Always Arabic
                        ExampleCard(
                            clues = getExampleClues3(),
                            answer = getExampleAnswer3(),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Start button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.25f)
                            )
                        ) {
                            Text(
                                text = getStartButtonText(language),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String, modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun SectionText(
    text: String, modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        modifier = modifier
    )
}

@Composable
private fun TutorialStep(
    number: String, text: String, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier, verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)
                ), contentAlignment = Alignment.Center
        ) {
            Text(
                text = number, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExampleCard(
    clues: List<String>, answer: String, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Clues
            clues.forEach { clue ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•",
                        color = Color(0xFF4CAF50),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = clue, color = Color.White, fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Answer
            Text(
                text = "→ $answer",
                color = Color(0xFF4CAF50),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Language-specific text functions
private fun getTutorialTitle(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "How to Play"
        Language.FRENCH -> "Comment jouer"
        Language.ARABIC -> "كيف تلعب"
    }
}

private fun getObjectiveTitle(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "🎯 Objective"
        Language.FRENCH -> "🎯 Objectif"
        Language.ARABIC -> "🎯 الهدف"
    }
}

private fun getObjectiveText(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "There are 5 clues hidden on the board. All 5 clues belong to a common category. Your objective is to guess the category in as few clue reveals as possible."
        Language.FRENCH -> "Il y a 5 indices cachés sur le plateau. Les 5 indices appartiennent à une catégorie commune. Votre objectif est de deviner la catégorie en révélant le moins d'indices possible."
        Language.ARABIC -> "هناك 5 أدلة مخفية على اللوحة. جميع الأدلة الخمسة تنتمي إلى فئة مشتركة. هدفك هو تخمين الفئة بأقل عدد ممكن من الأدلة المكشوفة."
    }
}

private fun getHowToPlayTitle(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "📖 How to Play"
        Language.FRENCH -> "📖 Comment jouer"
        Language.ARABIC -> "📖 طريقة اللعب"
    }
}

private fun getStep1Text(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Tap a tile to reveal a clue from the hidden board."
        Language.FRENCH -> "Appuyez sur une case pour révéler un indice du plateau caché."
        Language.ARABIC -> "اضغط على مربع لكشف دليل من اللوحة المخفية."
    }
}

private fun getStep2Text(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Type your guess for the category that connects all the clues."
        Language.FRENCH -> "Tapez votre réponse pour la catégorie qui relie tous les indices."
        Language.ARABIC -> "اكتب تخمينك للفئة التي تربط جميع الأدلة."
    }
}

private fun getStep3Text(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Each incorrect guess reveals the next clue. Win by guessing correctly in 5 tries or less!"
        Language.FRENCH -> "Chaque mauvaise réponse révèle l'indice suivant. Gagnez en devinant correctement en 5 essais ou moins !"
        Language.ARABIC -> "كل تخمين خاطئ يكشف الدليل التالي. اربح بالتخمين الصحيح في 5 محاولات أو أقل!"
    }
}

private fun getExampleTitle(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "💡 Examples"
        Language.FRENCH -> "💡 Exemples"
        Language.ARABIC -> "💡 أمثلة"
    }
}

// Example 1 - Always English
private fun getExampleClues1(): List<String> {
    return listOf("Lunch", "Sand", "Mail", "Safe deposit", "Think outside the")
}

private fun getExampleAnswer1(): String {
    return "Words that come before \"box\""
}

// Example 2 - Always French
private fun getExampleClues2(): List<String> {
    return listOf("Violon", "Guitare", "Cerf-volant", "Raquette de tennis", "Marionnette")
}

private fun getExampleAnswer2(): String {
    return "Choses avec des cordes"
}

// Example 3 - Always Arabic
private fun getExampleClues3(): List<String> {
    return listOf("قلي", "طبخ بطيء", "سلق", "خفق", "سلق صلب")
}

private fun getExampleAnswer3(): String {
    return "طرق تحضير البيض"
}

private fun getStartButtonText(language: Language): String {
    return when (language) {
        Language.ENGLISH -> "Got it! Let's Play 🎮"
        Language.FRENCH -> "Compris ! Jouons 🎮"
        Language.ARABIC -> "🎮 فهمت! لنلعب"
    }
}