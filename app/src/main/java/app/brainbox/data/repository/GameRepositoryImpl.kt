package app.brainbox.data.repository

import android.content.Context
import app.brainbox.data.network.CategoryNetworkService
import app.brainbox.domain.model.Category
import app.brainbox.domain.model.DailyChallenge
import app.brainbox.domain.repository.GameRepository
import app.brainbox.domain.repository.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GameRepositoryImpl(context: Context) : GameRepository {

    private val networkService = CategoryNetworkService()

    private val startDate = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 28, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override suspend fun getDailyChallenges(language: Language): Map<String, DailyChallenge> {
        val categoriesMap = loadCategories(language)
        val categories = categoriesMap.map { (name, items) -> Category(name, items) }
        return getCurrentAndNextChallenges(categories)
    }

    override fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private suspend fun loadCategories(language: Language): Map<String, List<String>> {
        return withContext(Dispatchers.IO) {
            println("Fetch forcé depuis le réseau pour $language (aucun cache)")
            networkService.fetchCategories(language)
        }
    }

    private fun getCurrentAndNextChallenges(categories: List<Category>): Map<String, DailyChallenge> {
        val challenges = mutableMapOf<String, DailyChallenge>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val today = Calendar.getInstance()
        val daysSinceStart = daysBetween(startDate, today)  // Jour 1 = 28/01/2026

        if (daysSinceStart < 1) {
            return emptyMap()
        }

        val todayIndex = daysSinceStart - 1  // index 0 = Jour 1

        if (todayIndex < categories.size) {
            val todayStr = dateFormat.format(today.time)
            challenges[todayStr] = DailyChallenge(
                date = todayStr,
                categories = listOf(categories[todayIndex])
            )
        }

        // Teasers futurs (optionnel – tu peux mettre 0 si tu veux seulement aujourd'hui)
        val maxTeaser = 6
        for (offset in 1..maxTeaser) {
            val futureIndex = todayIndex + offset
            if (futureIndex >= categories.size) break

            val futureCal = today.clone() as Calendar
            futureCal.add(Calendar.DAY_OF_MONTH, offset)
            val futureDateStr = dateFormat.format(futureCal.time)

            challenges[futureDateStr] = DailyChallenge(
                date = futureDateStr,
                categories = listOf(categories[futureIndex])
            )
        }

        return challenges
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        val startClean = start.clone() as Calendar
        val endClean = end.clone() as Calendar

        startClean.set(Calendar.HOUR_OF_DAY, 0)
        startClean.set(Calendar.MINUTE, 0)
        startClean.set(Calendar.SECOND, 0)
        startClean.set(Calendar.MILLISECOND, 0)

        endClean.set(Calendar.HOUR_OF_DAY, 0)
        endClean.set(Calendar.MINUTE, 0)
        endClean.set(Calendar.SECOND, 0)
        endClean.set(Calendar.MILLISECOND, 0)

        val diffMillis = endClean.timeInMillis - startClean.timeInMillis
        return (diffMillis / 86_400_000L).toInt() + 1  // +1 pour que Jour 1 = 28/01/2026
    }

    fun clearCache() {
        // Plus de cache → rien à faire
    }
}