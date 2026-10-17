package app.brainbox.presentation.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.data.player.PlayerRepository
import app.brainbox.domain.model.LeaderboardEntry
import app.brainbox.domain.model.LeaderboardPage
import app.brainbox.domain.model.LeaderboardPeriod
import app.brainbox.domain.repository.Language

@Composable
fun LeaderboardScreen(
    repository: PlayerRepository,
    todayNumber: Int,
    initialScope: Language?,
    onEditProfile: () -> Unit,
    onBack: () -> Unit
) {
    val texts = remember { PlayerTexts(Language.fromDeviceLocale()) }
    val profile by repository.profile.collectAsState()
    var period by rememberSaveable { mutableStateOf(LeaderboardPeriod.DAY) }
    // null = toutes les langues
    var scope by rememberSaveable { mutableStateOf(initialScope) }
    var page by remember { mutableStateOf<LeaderboardPage?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)

    LaunchedEffect(period, scope, reload) {
        page = null
        failed = false
        page = runCatching { repository.leaderboard(period, scope, todayNumber) }.getOrNull()
        failed = page == null
    }

    Box(modifier = Modifier.fillMaxSize().background(BrainBoxBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = texts.leaderboardTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                // Accès direct au profil depuis le classement
                Box(modifier = Modifier.clip(CircleShape).border(2.dp, Color(0xFFFFD93D), CircleShape)) {
                    IconButton(onClick = onEditProfile) { PlayerAvatar(profile.avatar, 40.dp) }
                }
            }

            Spacer(Modifier.height(16.dp))
            ChipRow(
                options = LeaderboardPeriod.entries,
                selected = period,
                label = texts::period,
                onSelect = { period = it }
            )
            Spacer(Modifier.height(8.dp))
            ChipRow(
                options = listOf<Language?>(null) + Language.entries,
                selected = scope,
                label = {
                    when (it) {
                        null -> "🌍 ${texts.allLanguages}"
                        Language.FRENCH -> "🇫🇷 FR"
                        Language.ENGLISH -> "🇬🇧 EN"
                        Language.ARABIC -> "🇸🇦 AR"
                    }
                },
                onSelect = { scope = it }
            )
            Spacer(Modifier.height(12.dp))

            val current = page
            when {
                current == null && !failed -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }

                current == null -> Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(texts.loadError, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { reload++ }) { Text(texts.retry) }
                }

                else -> {
                    if (current.cached) {
                        Text(texts.offline, fontSize = 12.sp, color = Color(0xFFFFB74D), modifier = Modifier.padding(bottom = 8.dp))
                    }
                    if (current.entries.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(texts.empty, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(current.entries, key = { it.uid }) { entry ->
                                LeaderboardRow(entry, isMe = entry.uid == profile.id, texts = texts)
                            }
                        }
                    }

                    // Ma position, toujours visible en bas (même hors du top 50).
                    Spacer(Modifier.height(12.dp))
                    Text(texts.myRank, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(4.dp))
                    val me = current.me
                    if (me != null) {
                        LeaderboardRow(me, isMe = true, texts = texts)
                    } else {
                        Text(
                            text = texts.notRanked,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFFFFD93D) else Color.White.copy(alpha = 0.12f))
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF2E1A47) else Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, isMe: Boolean, texts: PlayerTexts) {
    val medal = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isMe) Color(0xFFFFD93D).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f))
            .then(if (isMe) Modifier.border(2.dp, Color(0xFFFFD93D), RoundedCornerShape(16.dp)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = medal ?: "${entry.rank}",
            fontSize = if (medal != null) 24.sp else 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )
        PlayerAvatar(entry.avatar, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) "${entry.name} (${texts.you})" else entry.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = texts.wins(entry.wins),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Text(
            text = "${entry.score} ${texts.points}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFD93D)
        )
    }
}
