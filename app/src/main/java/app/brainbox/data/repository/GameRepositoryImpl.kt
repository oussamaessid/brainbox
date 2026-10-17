package app.brainbox.data.repository

import android.content.Context
import android.util.Log
import app.brainbox.data.cache.CategoryCacheManager
import app.brainbox.data.network.CategoryNetworkService
import app.brainbox.data.parser.CategoryParser
import app.brainbox.domain.model.Category
import app.brainbox.domain.model.DailyChallenge
import app.brainbox.domain.model.GameCalendar
import app.brainbox.domain.repository.GameRepository
import app.brainbox.domain.repository.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class GameRepositoryImpl(context: Context) : GameRepository {

    private val networkService = CategoryNetworkService()
    private val cache = CategoryCacheManager(context)
    private val clock = TrustedClock(context)
    private val calendar = GameCalendar()

    override suspend fun getDailyChallenge(language: Language): DailyChallenge? {
        val categories = loadCategories(language)
        if (categories.isEmpty()) return null

        val dayNumber = getChallengeNumber()
        if (dayNumber < 1) {
            Log.d(TAG, "Le jeu n'a pas encore commencé (jour $dayNumber)")
            return null
        }

        // Une entrée du fichier = un jour ; on reboucle quand la liste est épuisée.
        val category = categories[(dayNumber - 1) % categories.size]
        return DailyChallenge(date = getCurrentDate(), number = dayNumber, category = category)
    }

    override fun getCurrentDate(): String = calendar.formatDate(clock.now())

    override fun getChallengeNumber(): Int = calendar.dayNumber(clock.now())

    override fun millisUntilNextChallenge(): Long = calendar.millisUntilNextDay(clock.now())

    private suspend fun loadCategories(language: Language): List<Category> = withContext(Dispatchers.IO) {
        val fromNetwork = try {
            val response = networkService.fetchCategories(language)
            response.serverTimeMillis?.let(clock::sync)
            CategoryParser.parse(response.body).also {
                if (it.isNotEmpty()) cache.write(language, response.body)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Réseau indisponible, utilisation du cache : ${e.message}")
            emptyList()
        }

        fromNetwork.ifEmpty { cache.read(language)?.let(CategoryParser::parse).orEmpty() }
    }

    companion object {
        private const val TAG = "GameRepository"
    }
}

/**
 * Heure "de confiance" : l'heure du téléphone corrigée par l'écart mesuré avec le serveur
 * lors du dernier téléchargement. Empêche de jouer les défis futurs en avançant l'horloge
 * (tant que l'app a pu se connecter au moins une fois).
 */
class TrustedClock(context: Context) {

    private val prefs = context.getSharedPreferences("clock_prefs", Context.MODE_PRIVATE)

    fun now(): Long = System.currentTimeMillis() + prefs.getLong(KEY_OFFSET, 0L)

    fun sync(serverTimeMillis: Long) {
        val offset = serverTimeMillis - System.currentTimeMillis()
        // En dessous de 2 min, c'est juste la latence / la précision de l'en-tête.
        prefs.edit().putLong(KEY_OFFSET, if (abs(offset) < 120_000L) 0L else offset).apply()
    }

    companion object {
        private const val KEY_OFFSET = "server_offset_ms"
    }
}
