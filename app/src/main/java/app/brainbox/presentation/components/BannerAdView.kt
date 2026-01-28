package app.brainbox.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(adView: AdView) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { adView }
    )
}