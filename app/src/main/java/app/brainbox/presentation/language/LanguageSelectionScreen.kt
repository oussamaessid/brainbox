package app.brainbox.presentation.language

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.brainbox.domain.repository.Language
import app.brainbox.presentation.components.BannerAdView

@Composable
fun LanguageSelectionScreen(
    adManager: AdManager,
    onLanguageSelected: (Language) -> Unit
) {
    val bannerAd = remember { adManager.createBannerAdView(isLanguageScreen = true) }

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

            BannerAdView(bannerAd)
        }
    }
}

@Composable
fun LanguageCard(
    flag: String,
    language: String,
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
                Text(
                    text = language,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}