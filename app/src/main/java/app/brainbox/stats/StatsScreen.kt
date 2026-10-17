package app.brainbox.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.domain.model.PlayerStats
import app.brainbox.domain.repository.Language

private data class StatsTexts(
    val title: String,
    val played: String,
    val winRate: String,
    val streak: String,
    val maxStreak: String,
    val points: String,
    val distribution: String,
    val resetTitle: String,
    val resetMessage: String,
    val reset: String,
    val cancel: String
)

private fun statsTexts(language: Language) = when (language) {
    Language.FRENCH -> StatsTexts(
        "📊 STATISTIQUES", "Parties", "% victoires", "Série", "Record", "Points",
        "Victoires par nombre d'essais", "⚠️ Réinitialiser",
        "Remettre à zéro tous les scores et statistiques ? Cette action est irréversible.",
        "Réinitialiser", "Annuler"
    )
    Language.ENGLISH -> StatsTexts(
        "📊 STATISTICS", "Played", "Win %", "Streak", "Best", "Points",
        "Wins by number of guesses", "⚠️ Reset",
        "Reset all scores and statistics? This cannot be undone.",
        "Reset", "Cancel"
    )
    Language.ARABIC -> StatsTexts(
        "📊 الإحصائيات", "الألعاب", "% الفوز", "السلسلة", "الأفضل", "النقاط",
        "الانتصارات حسب عدد المحاولات", "⚠️ إعادة تعيين",
        "إعادة تعيين كل النقاط والإحصائيات؟ لا يمكن التراجع عن ذلك.",
        "إعادة تعيين", "إلغاء"
    )
}

@Composable
fun StatsScreen(
    stats: Map<Language, PlayerStats>,
    scores: Map<Language, Int>,
    todayNumber: Int,
    onBack: () -> Unit,
    onResetScores: () -> Unit = {}
) {
    var showResetDialog by remember { mutableStateOf(false) }
    val uiLanguage = remember { Language.fromDeviceLocale() }
    val texts = statsTexts(uiLanguage)

    BackHandler(onBack = onBack)

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
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = texts.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252).copy(alpha = 0.3f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Reset",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Language.entries.forEach { language ->
                    LanguageStatsCard(
                        language = language,
                        stats = stats[language] ?: PlayerStats(),
                        score = scores[language] ?: 0,
                        todayNumber = todayNumber,
                        texts = texts
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = texts.resetTitle, fontWeight = FontWeight.Bold) },
            text = { Text(texts.resetMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetScores()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text(texts.reset)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(texts.cancel)
                }
            }
        )
    }
}

@Composable
private fun LanguageStatsCard(
    language: Language,
    stats: PlayerStats,
    score: Int,
    todayNumber: Int,
    texts: StatsTexts
) {
    val (flag, name) = when (language) {
        Language.FRENCH -> "🇫🇷" to "Français"
        Language.ENGLISH -> "🇬🇧" to "English"
        Language.ARABIC -> "🇸🇦" to "العربية"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "🏆 $score",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD93D)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell("${stats.played}", texts.played, Modifier.weight(1f))
                StatCell("${stats.winRate}", texts.winRate, Modifier.weight(1f))
                StatCell("🔥${stats.streakOn(todayNumber)}", texts.streak, Modifier.weight(1f))
                StatCell("${stats.maxStreak}", texts.maxStreak, Modifier.weight(1f))
            }

            if (stats.distribution.any { it > 0 }) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = texts.distribution,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                val max = stats.distribution.max().coerceAtLeast(1)
                stats.distribution.forEachIndexed { index, count ->
                    DistributionBar(guesses = index + 1, count = count, fraction = count.toFloat() / max)
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun DistributionBar(guesses: Int, count: Int, fraction: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$guesses",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(16.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.08f, 1f))
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (count > 0) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}
