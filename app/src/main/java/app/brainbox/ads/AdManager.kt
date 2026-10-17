package app.brainbox.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import app.brainbox.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class AdManager(private val context: Context) {

    companion object {
        private const val INTERSTITIAL_AD_ID = "ca-app-pub-9651830078758870/7265331247"
        private const val APP_OPEN_AD_ID = "ca-app-pub-9651830078758870/2715397515"
        private const val BANNER_LANGUAGE_ID = "ca-app-pub-9651830078758870/1501868672"
        private const val BANNER_GAME_ID = "ca-app-pub-9651830078758870/7875705331"
        // TODO: créer un bloc "Avec récompense" dans AdMob et coller son ID ici.
        // Tant qu'il est vide, le bouton "Indice" est masqué en release.
        private const val REWARDED_AD_ID = ""

        private const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
        private const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"

        private const val TAG = "AdManager"
        private val USE_TEST_ADS = BuildConfig.DEBUG

        private const val MAX_INTERSTITIALS_PER_DAY = 3
        private const val INTERSTITIAL_INTERVAL = 3 * 60 * 1000L
        // Minimum 4 heures entre deux App Open Ads pour éviter la sur-exposition
        private const val APP_OPEN_AD_COOLDOWN = 4 * 60 * 60 * 1000L
        private const val PREF_LAST_APP_OPEN = "last_app_open_ms"
        private const val PREF_INTERSTITIAL_DAY = "interstitial_day"
        private const val PREF_INTERSTITIAL_COUNT = "interstitial_count"
        // Si l'App Open arrive plus de 4 s après le lancement, l'utilisateur tape déjà
        // dans l'app → on ne l'affiche pas (sinon clic accidentel = trafic invalide).
        private const val APP_OPEN_MAX_WAIT_MS = 4_000L
    }

    private val prefs = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
    // Mesuré depuis le lancement (et non depuis la requête) : le consentement RGPD
    // et l'init du SDK retardent la requête, la pub arriverait en pleine partie.
    private val createdAt = System.currentTimeMillis()

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedRetries = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastInterstitialTime = 0L
    // Persisté : sinon le délai de 4 h repart à zéro à chaque lancement de l'app.
    private var lastAppOpenAdTime: Long
        get() = prefs.getLong(PREF_LAST_APP_OPEN, 0L)
        set(value) = prefs.edit().putLong(PREF_LAST_APP_OPEN, value).apply()

    /** true une fois le consentement obtenu et le SDK initialisé : aucune pub n'est demandée avant. */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    val isRewardedAvailable: Boolean get() = USE_TEST_ADS || REWARDED_AD_ID.isNotEmpty()

    /** À appeler après le consentement UMP (ConsentManager.canRequestAds). */
    fun initialize(onReady: () -> Unit = {}) {
        if (_isReady.value) {
            onReady()
            return
        }
        if (USE_TEST_ADS) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build()
            )
        }
        MobileAds.initialize(context) {
            Log.d(TAG, "AdMob SDK initialized")
            if (!_isReady.value) {
                _isReady.value = true
                loadInterstitialAd()
                loadRewardedAd()
            }
            onReady()
        }
    }

    // ==================== INTERSTITIAL ====================

    private fun canShowInterstitialToday(): Boolean {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (prefs.getInt(PREF_INTERSTITIAL_DAY, -1) != today) {
            prefs.edit().putInt(PREF_INTERSTITIAL_DAY, today).putInt(PREF_INTERSTITIAL_COUNT, 0).apply()
        }
        return prefs.getInt(PREF_INTERSTITIAL_COUNT, 0) < MAX_INTERSTITIALS_PER_DAY
    }

    private fun loadInterstitialAd() {
        if (!_isReady.value) return
        val adId = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_ID else INTERSTITIAL_AD_ID

        InterstitialAd.load(context, adId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "Interstitial Ad failed to load: ${loadAdError.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (!canShowInterstitialToday()) {
            onAdDismissed()
            return
        }

        val timeSinceLast = System.currentTimeMillis() - lastInterstitialTime
        if (lastInterstitialTime > 0 && timeSinceLast < INTERSTITIAL_INTERVAL) {
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onAdDismissed()
            loadInterstitialAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
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
                lastInterstitialTime = System.currentTimeMillis()
                prefs.edit()
                    .putInt(PREF_INTERSTITIAL_COUNT, prefs.getInt(PREF_INTERSTITIAL_COUNT, 0) + 1)
                    .apply()
            }
        }
        ad.show(activity)
    }

    // ==================== APP OPEN ====================

    fun loadAppOpenAd(onAdLoaded: () -> Unit = {}) {
        if (!_isReady.value) return
        val adId = if (USE_TEST_ADS) TEST_APP_OPEN_AD_ID else APP_OPEN_AD_ID

        AppOpenAd.load(context, adId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    if (System.currentTimeMillis() - createdAt > APP_OPEN_MAX_WAIT_MS) {
                        Log.d(TAG, "App Open Ad loaded too late. Not shown.")
                        return
                    }
                    onAdLoaded()
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    appOpenAd = null
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val timeSinceLast = System.currentTimeMillis() - lastAppOpenAdTime
        if (lastAppOpenAdTime > 0 && timeSinceLast < APP_OPEN_AD_COOLDOWN) {
            onAdDismissed()
            return
        }

        // Jamais par-dessus une activité fermée ou passée en arrière-plan.
        val resumed = (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState
            ?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) ?: true
        if (activity.isFinishing || activity.isDestroyed || !resumed) {
            onAdDismissed()
            return
        }

        val ad = appOpenAd
        if (ad == null) {
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                onAdDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                onAdDismissed()
            }
            override fun onAdShowedFullScreenContent() {
                lastAppOpenAdTime = System.currentTimeMillis()
            }
        }
        ad.show(activity)
    }

    // ==================== REWARDED (indice) ====================

    private fun loadRewardedAd() {
        if (!_isReady.value || !isRewardedAvailable || rewardedAd != null) return
        val adId = if (USE_TEST_ADS) TEST_REWARDED_AD_ID else REWARDED_AD_ID

        RewardedAd.load(context, adId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedRetries = 0
                    _isRewardedReady.value = true
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "Rewarded Ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    // Nouvel essai (30 s, 60 s, 90 s) : sinon le bouton "Indice" reste grisé.
                    if (rewardedRetries < 3) {
                        rewardedRetries++
                        handler.postDelayed({ loadRewardedAd() }, rewardedRetries * 30_000L)
                    }
                }
            }
        )
    }

    /** [onRewarded] n'est appelé que si la vidéo a été regardée jusqu'au bout. */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd ?: return loadRewardedAd()

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedReady.value = false
                loadRewardedAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _isRewardedReady.value = false
                loadRewardedAd()
            }
        }
        ad.show(activity) { onRewarded() }
    }

    // ==================== BANNER ====================

    fun createBannerAdView(isLanguageScreen: Boolean): AdView {
        val adView = AdView(context)
        adView.adUnitId = when {
            USE_TEST_ADS -> TEST_BANNER_AD_ID
            isLanguageScreen -> BANNER_LANGUAGE_ID
            else -> BANNER_GAME_ID
        }
        adView.setAdSize(AdSize.BANNER)
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d(TAG, "Banner Ad failed to load: ${loadAdError.message}")
            }
        }
        return adView
    }
}
