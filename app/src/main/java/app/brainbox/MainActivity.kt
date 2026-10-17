package app.brainbox

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainbox.ads.AdManager
import app.brainbox.ads.ConsentManager
import androidx.lifecycle.lifecycleScope
import app.brainbox.data.player.PlayerRepository
import app.brainbox.data.repository.GameRepositoryImpl
import app.brainbox.presentation.player.LeaderboardScreen
import app.brainbox.presentation.player.ProfileScreen
import kotlinx.coroutines.launch
import app.brainbox.domain.repository.Language
import app.brainbox.notifications.DailyReminderWorker
import app.brainbox.presentation.components.NextChallengeCountdown
import app.brainbox.presentation.components.TutorialDialog
import app.brainbox.presentation.components.getShareText
import app.brainbox.presentation.game.GameScreen
import app.brainbox.presentation.game.GameViewModel
import app.brainbox.presentation.language.LanguageSelectionScreen
import app.brainbox.stats.StatsScreen
import app.brainbox.ui.theme.BrainBoxTheme
import app.brainbox.utils.PreferencesManager
import app.brainbox.utils.shareText

class MainActivity : ComponentActivity() {
    // Mise à jour obligatoire : bloque l'app tant qu'une version plus récente existe sur le Play Store.
    private val forceUpdate = ForceUpdateManager(this)

    private val preferencesManager by lazy { PreferencesManager(applicationContext) }
    private val playerRepository by lazy { (application as BrainBoxApplication).playerRepository }

    // Survit aux rotations / changements de configuration (l'état de la partie n'est pas perdu).
    private val viewModel: GameViewModel by viewModels {
        GameViewModel.factory(GameRepositoryImpl(applicationContext), preferencesManager, playerRepository)
    }

    private lateinit var adManager: AdManager
    private lateinit var consentManager: ConsentManager

    override fun onResume() {
        super.onResume()
        forceUpdate.resumeIfInProgress()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forceUpdate.check { }

        adManager = AdManager(this)
        consentManager = ConsentManager(this)
        setContent {
            BrainBoxTheme(darkTheme = false, dynamicColor = false) {
                BrainBoxApp(
                    viewModel = viewModel,
                    adManager = adManager,
                    preferencesManager = preferencesManager,
                    playerRepository = playerRepository,
                    onPrivacyOptions = if (privacyOptionsRequired.value) {
                        { consentManager.showPrivacyOptionsForm(this) }
                    } else null
                )
            }
        }

        // Pas d'App Open Ad sur rotation (savedInstanceState != null) ni au tout premier
        // lancement : le tutoriel est affiché et le joueur est en train de taper dessus.
        val showAppOpenAd = savedInstanceState == null && !preferencesManager.isFirstLaunch()

        // Consentement RGPD d'abord, pubs ensuite. Un utilisateur qui a déjà répondu
        // peut recevoir des pubs tout de suite, sans attendre la mise à jour du consentement.
        var adsStarted = false
        val onConsentReady = {
            if (!adsStarted && consentManager.canRequestAds) {
                adsStarted = true
                adManager.initialize {
                    if (showAppOpenAd) adManager.loadAppOpenAd { adManager.showAppOpenAd(this) }
                }
            }
        }
        if (consentManager.canRequestAds) onConsentReady()
        consentManager.gather(this) {
            onConsentReady()
            privacyOptionsRequired.value = consentManager.isPrivacyOptionsRequired
        }

        DailyReminderWorker.schedule(applicationContext)

        // Victoires restées en attente (hors-ligne) : envoyées au classement dès que possible.
        lifecycleScope.launch { playerRepository.sync() }
    }

    private val privacyOptionsRequired = mutableStateOf(false)
}

@Composable
fun BrainBoxApp(
    viewModel: GameViewModel,
    adManager: AdManager,
    preferencesManager: PreferencesManager,
    playerRepository: PlayerRepository,
    onPrivacyOptions: (() -> Unit)?
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedLanguage by rememberSaveable { mutableStateOf<Language?>(null) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showLeaderboard by rememberSaveable { mutableStateOf(false) }
    var leaderboardScope by rememberSaveable { mutableStateOf<Language?>(null) }
    val profile by playerRepository.profile.collectAsState()
    var completedLanguage by rememberSaveable { mutableStateOf<Language?>(null) }
    // Incrémenté au retour au menu pour rafraîchir les badges (série, "joué aujourd'hui").
    var menuRefresh by remember { mutableIntStateOf(0) }

    val isFirstLaunch = remember { preferencesManager.isFirstLaunch() }
    var showTutorial by remember { mutableStateOf(isFirstLaunch) }
    // Bienvenue + profil créé automatiquement : une seule fois, après le tutoriel.
    var showWelcome by rememberSaveable { mutableStateOf(playerRepository.needsSetup) }

    // Notifications (Android 13+) : demandé une seule fois, après la première partie
    // terminée plutôt qu'au lancement, quand le joueur comprend l'intérêt du rappel.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    fun askNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !preferencesManager.wasNotificationPermissionAsked()
        ) {
            preferencesManager.setNotificationPermissionAsked()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun backToMenu() {
        menuRefresh++
        val playedToday = Language.entries.any { viewModel.isGameCompleted(it) }
        val goToMenu = {
            selectedLanguage = null
            if (playedToday) askNotificationPermissionOnce()
        }
        // Afficher un interstitiel à la transition naturelle (retour au menu)
        if (activity != null) adManager.showInterstitialAd(activity, goToMenu) else goToMenu()
    }

    when {
        showWelcome && !showTutorial -> ProfileScreen(
            repository = playerRepository,
            setup = true,
            onDone = { showWelcome = false }
        )

        showProfile -> ProfileScreen(
            repository = playerRepository,
            setup = false,
            onDone = { showProfile = false }
        )

        showLeaderboard -> LeaderboardScreen(
            repository = playerRepository,
            todayNumber = viewModel.todayNumber(),
            initialScope = leaderboardScope,
            onEditProfile = { showProfile = true },
            onBack = { showLeaderboard = false }
        )

        showStats -> StatsScreen(
            stats = remember(menuRefresh) { viewModel.getAllStats() },
            scores = remember(menuRefresh) { viewModel.getAllScores() },
            todayNumber = viewModel.todayNumber(),
            onBack = { showStats = false },
            onResetScores = {
                viewModel.resetScoresAndStats()
                menuRefresh++
            }
        )

        selectedLanguage == null -> {
            val badges = remember(menuRefresh) {
                Language.entries.associateWith { languageBadge(viewModel, it) }
            }
            LanguageSelectionScreen(
                adManager = adManager,
                badgeFor = { badges[it] },
                onLanguageSelected = { language ->
                    if (viewModel.isGameCompleted(language)) {
                        completedLanguage = language
                    } else {
                        // Précharger le challenge AVANT de naviguer vers GameScreen.
                        // Ainsi quand GameScreen apparaît, isLoading=true et currentCategory=null
                        // dès le début → le LoadingScreen s'affiche immédiatement, sans flash.
                        viewModel.loadDailyChallenge(language)
                        selectedLanguage = language
                    }
                },
                onShowStats = {
                    menuRefresh++
                    showStats = true
                },
                profile = profile,
                onShowProfile = { showProfile = true },
                onShowLeaderboard = {
                    leaderboardScope = null
                    showLeaderboard = true
                },
                onPrivacyOptions = onPrivacyOptions
            )

            completedLanguage?.let { language ->
                CompletedGameDialog(
                    language = language,
                    isWin = viewModel.wasGameWon(language),
                    totalScore = viewModel.getTotalScore(language),
                    millisUntilNext = viewModel::millisUntilNextChallenge,
                    onShare = { shareText(context, viewModel.buildShareText(language)) },
                    onDismiss = { completedLanguage = null }
                )
            }
        }

        else -> GameScreen(
            viewModel = viewModel,
            adManager = adManager,
            language = selectedLanguage!!,
            onBackToMenu = ::backToMenu,
            onShowLeaderboard = {
                // Le classement s'ouvre sur la langue jouée ; le retour mène au menu.
                leaderboardScope = selectedLanguage
                menuRefresh++
                selectedLanguage = null
                showLeaderboard = true
            }
        )
    }

    if (showTutorial) {
        TutorialDialog(
            language = Language.fromDeviceLocale(),
            onDismiss = {
                showTutorial = false
                if (isFirstLaunch) {
                    preferencesManager.setFirstLaunchComplete()
                }
            }
        )
    }
}

private fun languageBadge(viewModel: GameViewModel, language: Language): String? {
    val streak = viewModel.getAllStats()[language]?.streakOn(viewModel.todayNumber()) ?: 0
    val played = viewModel.isGameCompleted(language)
    return when {
        played && streak > 0 -> "✅ 🔥 $streak"
        played -> "✅"
        streak > 0 -> "🔥 $streak"
        else -> null
    }
}

@Composable
fun CompletedGameDialog(
    language: Language,
    isWin: Boolean,
    totalScore: Int,
    millisUntilNext: () -> Long,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = if (isWin) listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF388E3C),
                                Color(0xFF1B5E20)
                            ) else listOf(
                                Color(0xFFE91E63),
                                Color(0xFFC2185B),
                                Color(0xFF880E4F)
                            ),
                            radius = 800f
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isWin) "🏆" else "💔",
                        fontSize = 72.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = if (isWin) getWinTitle(language) else getLoseTitle(language),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(3.dp)
                            .background(
                                Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp)
                            )
                            .padding(bottom = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = getCompletedMessage(language, isWin),
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🏆", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = getTotalScoreText(language),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$totalScore",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NextChallengeCountdown(
                        language = language,
                        millisUntilNext = millisUntilNext,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = "📤 ${getShareText(language)}",
                            color = if (isWin) Color(0xFF1B5E20) else Color(0xFF880E4F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = getOkText(language),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun getWinTitle(language: Language): String {
    return when (language) {
        Language.FRENCH -> "GAGNÉ"
        Language.ENGLISH -> "WON"
        Language.ARABIC -> "فوز"
    }
}

fun getLoseTitle(language: Language): String {
    return when (language) {
        Language.FRENCH -> "PERDU"
        Language.ENGLISH -> "LOST"
        Language.ARABIC -> "خسارة"
    }
}

fun getCompletedMessage(language: Language, isWin: Boolean): String {
    return when (language) {
        Language.FRENCH -> if (isWin)
            "Vous avez déjà gagné ce jeu aujourd'hui !"
        else
            "Vous avez déjà perdu ce jeu aujourd'hui. Revenez demain !"
        Language.ENGLISH -> if (isWin)
            "You already won this game today!"
        else
            "You already lost this game today. Come back tomorrow!"
        Language.ARABIC -> if (isWin)
            "!لقد فزت بهذه اللعبة اليوم بالفعل"
        else
            "!لقد خسرت هذه اللعبة اليوم. عد غداً"
    }
}

fun getTotalScoreText(language: Language): String {
    return when (language) {
        Language.FRENCH -> "Score total:"
        Language.ENGLISH -> "Total score:"
        Language.ARABIC -> ":النقاط الكلية"
    }
}

fun getOkText(language: Language): String {
    return when (language) {
        Language.FRENCH -> "D'accord"
        Language.ENGLISH -> "OK"
        Language.ARABIC -> "حسناً"
    }
}