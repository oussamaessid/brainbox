package app.brainbox.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView

// Hauteur fixe (taille d'AdSize.BANNER) : l'espace est réservé avant que la pub arrive,
// donc le clavier ne remonte pas sous le doigt du joueur (clic accidentel).
@Composable
fun BannerAdView(adView: AdView) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        factory = { adView }
    )
}