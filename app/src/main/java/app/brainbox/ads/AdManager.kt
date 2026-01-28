package app.brainbox.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdManager(private val context: Context) {

    companion object {
        // IDs de production
        private const val APP_ID = "ca-app-pub-4161995857939030~6951034960"
        private const val INTERSTITIAL_AD_ID = "ca-app-pub-4161995857939030/5044665156"
        private const val APP_OPEN_AD_ID = "ca-app-pub-4161995857939030/9796290779"
        private const val BANNER_LANGUAGE_ID = "ca-app-pub-4161995857939030/5007397901"
        private const val BANNER_GAME_ID = "ca-app-pub-4161995857939030/3694316236"

        // IDs de test
        private const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/9214589741"

        private const val TAG = "AdManager"

        // Mode test/production
        private const val USE_TEST_ADS = true // Changez à false pour production
    }

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var lastInterstitialTime = 0L
    private val interstitialInterval = 5 * 60 * 1000L // 5 minutes en millisecondes

    init {
        // Initialiser Mobile Ads SDK
        MobileAds.initialize(context) {
            Log.d(TAG, "AdMob SDK initialized")
        }

        // Ajouter ID de test device si nécessaire
        if (USE_TEST_ADS) {
            val testDeviceIds = listOf(
                AdRequest.DEVICE_ID_EMULATOR,
                // Ajoutez votre ID de test device ici
                // "YOUR_TEST_DEVICE_ID"
            )
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }

        // Charger l'interstitielle
        loadInterstitialAd()

        // Démarrer le timer pour les interstitielles
        startInterstitialTimer()
    }

    // === ANNONCE À L'OUVERTURE (APP OPEN AD) ===
    fun loadAppOpenAd(onAdLoaded: () -> Unit = {}) {
        val adRequest = AdRequest.Builder().build()
        val adId = if (USE_TEST_ADS) TEST_APP_OPEN_AD_ID else APP_OPEN_AD_ID

        AppOpenAd.load(
            context,
            adId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App Open Ad loaded")
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    appOpenAd = null
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (appOpenAd != null) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    onAdDismissed()
                    Log.d(TAG, "App Open Ad dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open Ad showed")
                }
            }

            appOpenAd?.show(activity)
        } else {
            Log.d(TAG, "App Open Ad not ready")
            onAdDismissed()
            // Recharger pour la prochaine fois
            loadAppOpenAd()
        }
    }

    // === ANNONCE INTERSTITIELLE ===
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        val adId = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_ID else INTERSTITIAL_AD_ID

        InterstitialAd.load(
            context,
            adId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad failed to load: ${loadAdError.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd() // Recharger pour la prochaine fois
                    onAdDismissed()
                    Log.d(TAG, "Interstitial Ad dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    Log.e(TAG, "Interstitial Ad failed to show: ${adError.message}")
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad showed")
                }
            }

            interstitialAd?.show(activity)
            lastInterstitialTime = System.currentTimeMillis()
        } else {
            Log.d(TAG, "Interstitial Ad not ready")
            onAdDismissed()
        }
    }

    // Timer pour afficher l'interstitielle toutes les 5 minutes
    private fun startInterstitialTimer() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(interstitialInterval)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastInterstitialTime >= interstitialInterval) {
                    // Afficher l'interstitielle si disponible
                    if (interstitialAd != null && context is Activity) {
                        showInterstitialAd(context)
                    }
                }
            }
        }
    }

    // === BANNIÈRE ===
    fun getBannerAdId(isLanguageScreen: Boolean): String {
        return if (USE_TEST_ADS) {
            TEST_BANNER_AD_ID
        } else {
            if (isLanguageScreen) BANNER_LANGUAGE_ID else BANNER_GAME_ID
        }
    }

    fun createBannerAdView(isLanguageScreen: Boolean): AdView {
        val adView = AdView(context)
        adView.adUnitId = getBannerAdId(isLanguageScreen)
        adView.setAdSize(AdSize.BANNER)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner Ad loaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG, "Banner Ad failed to load: ${loadAdError.message}")
            }
        }

        return adView
    }
}