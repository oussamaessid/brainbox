package app.brainbox.presentation.language

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.ads.AdManager
import app.brainbox.domain.model.PlayerProfile
import app.brainbox.domain.repository.Language
import app.brainbox.presentation.player.PlayerAvatar
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import app.brainbox.presentation.components.BannerAdView
import app.brainbox.presentation.components.TutorialDialog

@Composable
fun LanguageSelectionScreen(
    adManager: AdManager,
    badgeFor: (Language) -> String?,
    onLanguageSelected: (Language) -> Unit,
    onShowStats: () -> Unit,
    profile: PlayerProfile,
    onShowProfile: () -> Unit,
    onShowLeaderboard: () -> Unit,
    onPrivacyOptions: (() -> Unit)? = null
) {
    var showTutorial by remember { mutableStateOf(false) }
    val uiLanguage = remember { Language.fromDeviceLocale() }

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
        ) {
            // Contenu principal
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Stats + classement (gauche), profil (centre), aide (droite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(
                            onClick = onShowStats,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text(text = "📊", fontSize = 28.sp)
                        }
                        IconButton(
                            onClick = onShowLeaderboard,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text(text = "🏆", fontSize = 28.sp)
                        }
                    }
                    ProfileChip(
                        profile = profile,
                        onClick = onShowProfile,
                        modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { showTutorial = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "How to Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                    badge = badgeFor(Language.ENGLISH),
                    onClick = { onLanguageSelected(Language.ENGLISH) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LanguageCard(
                    flag = "🇫🇷",
                    language = "Français",
                    badge = badgeFor(Language.FRENCH),
                    onClick = { onLanguageSelected(Language.FRENCH) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                LanguageCard(
                    flag = "🇸🇦",
                    language = "العربية",
                    badge = badgeFor(Language.ARABIC),
                    onClick = { onLanguageSelected(Language.ARABIC) }
                )
            }

            if (onPrivacyOptions != null) {
                TextButton(
                    onClick = onPrivacyOptions,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = when (uiLanguage) {
                            Language.FRENCH -> "Paramètres de confidentialité"
                            Language.ENGLISH -> "Privacy settings"
                            Language.ARABIC -> "إعدادات الخصوصية"
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            // Banner Ad with horizontal margins
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
            ) {
                BannerAdView(adManager, isLanguageScreen = true)
            }
        }

        // Tutorial Dialog
        if (showTutorial) {
            TutorialDialog(
                language = uiLanguage,
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Composable
private fun ProfileChip(profile: PlayerProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAvatar(profile.avatar, 36.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = profile.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = " ✏️", fontSize = 12.sp)
    }
}

@Composable
fun LanguageCard(
    flag: String,
    language: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = flag,
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = language,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (badge != null) {
                        Text(
                            text = badge,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD93D)
                        )
                    }
                }
            }
        }
    }
}