package app.brainbox.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.brainbox.ads.AdManager
import com.google.android.gms.ads.AdRequest

// Hauteur fixe (taille d'AdSize.BANNER) : l'espace est réservé avant que la pub arrive,
// donc le clavier ne remonte pas sous le doigt du joueur (clic accidentel).
@Composable
fun BannerAdView(adManager: AdManager, isLanguageScreen: Boolean) {
    val adView = remember { adManager.createBannerAdView(isLanguageScreen) }
    val isReady by adManager.isReady.collectAsState()

    // La pub n'est demandée qu'après le consentement RGPD.
    LaunchedEffect(isReady) {
        if (isReady) adView.loadAd(AdRequest.Builder().build())
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        factory = { adView }
    )
}
