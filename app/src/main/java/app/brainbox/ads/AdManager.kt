package app.brainbox.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import app.brainbox.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Calendar

class AdManager(private val context: Context) {

    companion object {
        private const val INTERSTITIAL_AD_ID = "ca-app-pub-9651830078758870/7265331247"
        private const val APP_OPEN_AD_ID = "ca-app-pub-9651830078758870/2715397515"
        private const val BANNER_LANGUAGE_ID = "ca-app-pub-9651830078758870/1501868672"
        private const val BANNER_GAME_ID = "ca-app-pub-9651830078758870/7875705331"

        private const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/9214589741"

        private const val TAG = "AdManager"
        // En debug = annonces test, en release = vraies annonces (jamais cliquer sur ses propres pubs)
        private val USE_TEST_ADS = BuildConfig.DEBUG

        private const val MAX_INTERSTITIALS_PER_DAY = 3
        // Minimum 3 minutes entre deux interstitiels
        private const val INTERSTITIAL_INTERVAL = 3 * 60 * 1000L
    }

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var lastInterstitialTime = 0L

    private var dailyInterstitialCount = 0
    private var lastResetDay = -1

    init {
        MobileAds.initialize(context) {
            Log.d(TAG, "AdMob SDK initialized")
        }

        if (USE_TEST_ADS) {
            val testDeviceIds = listOf(AdRequest.DEVICE_ID_EMULATOR)
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }

        loadInterstitialAd()
        // Le timer automatique a été supprimé : les interstitiels ne doivent s'afficher
        // qu'aux transitions naturelles (fin de partie), jamais automatiquement.
    }

    private fun resetDailyCountIfNeeded() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (today != lastResetDay) {
            dailyInterstitialCount = 0
            lastResetDay = today
            Log.d(TAG, "Daily interstitial counter reset")
        }
    }

    private fun canShowInterstitialToday(): Boolean {
        resetDailyCountIfNeeded()
        return dailyInterstitialCount < MAX_INTERSTITIALS_PER_DAY
    }

    fun loadAppOpenAd(onAdLoaded: () -> Unit = {}) {
        val adRequest = AdRequest.Builder().build()
        val adId = if (USE_TEST_ADS) TEST_APP_OPEN_AD_ID else APP_OPEN_AD_ID

        AppOpenAd.load(context, adId, adRequest,
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
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    onAdDismissed()
                }
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open Ad showed")
                }
            }
            appOpenAd?.show(activity)
        } else {
            onAdDismissed()
            loadAppOpenAd()
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        val adId = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_ID else INTERSTITIAL_AD_ID

        InterstitialAd.load(context, adId, adRequest,
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
        if (!canShowInterstitialToday()) {
            Log.d(TAG, "Daily limit reached ($MAX_INTERSTITIALS_PER_DAY/day). Ad skipped.")
            onAdDismissed()
            return
        }

        val timeSinceLast = System.currentTimeMillis() - lastInterstitialTime
        if (lastInterstitialTime > 0 && timeSinceLast < INTERSTITIAL_INTERVAL) {
            Log.d(TAG, "Too soon since last interstitial. Ad skipped.")
            onAdDismissed()
            return
        }

        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }
                override fun onAdShowedFullScreenContent() {
                    dailyInterstitialCount++
                    lastInterstitialTime = System.currentTimeMillis()
                    Log.d(TAG, "Interstitial shown. Count: $dailyInterstitialCount/$MAX_INTERSTITIALS_PER_DAY")
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial Ad not ready")
            onAdDismissed()
        }
    }

    fun getBannerAdId(isLanguageScreen: Boolean): String {
        return if (USE_TEST_ADS) TEST_BANNER_AD_ID
        else if (isLanguageScreen) BANNER_LANGUAGE_ID else BANNER_GAME_ID
    }

    fun createBannerAdView(isLanguageScreen: Boolean): AdView {
        val adView = AdView(context)
        adView.adUnitId = getBannerAdId(isLanguageScreen)
        adView.setAdSize(AdSize.BANNER)
        adView.loadAd(AdRequest.Builder().build())
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() { Log.d(TAG, "Banner Ad loaded") }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG, "Banner Ad failed to load: ${loadAdError.message}")
            }
        }
        return adView
    }
}