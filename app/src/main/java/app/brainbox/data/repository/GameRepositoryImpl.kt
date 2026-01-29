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

    // 🎯 DATE DE DÉMARRAGE : 28 JANVIER 2026
    private val startDate = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 28, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override suspend fun getDailyChallenges(language: Language): Map<String, DailyChallenge> {
        val categoriesMap = loadCategories(language)

        if (categoriesMap.isEmpty()) {
            println("❌ AUCUNE CATÉGORIE CHARGÉE depuis le réseau!")
            return emptyMap()
        }

        val categories = categoriesMap.map { (name, items) -> Category(name, items) }
        return getCurrentAndNextChallenges(categories)
    }

    override fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private suspend fun loadCategories(language: Language): Map<String, List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                println("📡 Début fetch depuis le réseau pour $language")
                val categories = networkService.fetchCategories(language)
                println("✅ Fetch réussi: ${categories.size} catégories récupérées")

                categories.entries.take(5).forEachIndexed { index, entry ->
                    println("   [$index] ${entry.key}: ${entry.value.size} items")
                }

                if (categories.size > 5) {
                    println("   ... et ${categories.size - 5} autres catégories")
                }

                categories
            } catch (e: Exception) {
                println("💥 Erreur lors du fetch: ${e.message}")
                e.printStackTrace()
                emptyMap()
            }
        }
    }

    private fun getCurrentAndNextChallenges(categories: List<Category>): Map<String, DailyChallenge> {
        val challenges = mutableMapOf<String, DailyChallenge>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val today = Calendar.getInstance()
        val daysSinceStart = daysBetween(startDate, today)

        // 🔍 DEBUGGING DÉTAILLÉ
        println("═══════════════════════════════════════")
        println("🗓️  CALCUL DES DÉFIS")
        println("═══════════════════════════════════════")
        println("📅 Date de démarrage  : ${dateFormat.format(startDate.time)}")
        println("📅 Date d'aujourd'hui : ${dateFormat.format(today.time)}")
        println("📊 Jours depuis début : $daysSinceStart")
        println("📚 Catégories dispo   : ${categories.size}")

        // Debug des timestamps
        val startMillis = startDate.timeInMillis
        val todayMillis = today.timeInMillis
        val diffMillis = todayMillis - startMillis
        val diffDays = diffMillis / 86_400_000L
        println("🕐 Start timestamp    : $startMillis")
        println("🕐 Today timestamp    : $todayMillis")
        println("🕐 Diff millis        : $diffMillis")
        println("🕐 Diff days (calc)   : $diffDays")
        println("═══════════════════════════════════════")

        if (daysSinceStart < 1) {
            println("⚠️ ERREUR: Le jeu n'a pas encore commencé!")
            println("   Il commence le ${dateFormat.format(startDate.time)}")
            println("═══════════════════════════════════════")
            return emptyMap()
        }

        val todayIndex = daysSinceStart - 1  // Jour 1 = index 0
        println("🎯 Index calculé      : $todayIndex")

        if (todayIndex >= 0 && todayIndex < categories.size) {
            val todayStr = dateFormat.format(today.time)
            val category = categories[todayIndex]

            println("✅ CATÉGORIE DU JOUR TROUVÉE!")
            println("   Date   : $todayStr")
            println("   Nom    : '${category.name}'")
            println("   Items  : ${category.items.size} éléments")
            println("   Liste  : ${category.items.joinToString(", ")}")

            challenges[todayStr] = DailyChallenge(
                date = todayStr,
                categories = listOf(category)
            )
        } else {
            println("❌ ERREUR: Index $todayIndex hors limites!")
            println("   Catégories disponibles: 0 à ${categories.size - 1}")
            if (todayIndex >= categories.size) {
                println("   ⚠️ Vous avez épuisé toutes les catégories!")
            }
        }

        // Créer des défis pour les 6 prochains jours (teasers)
        println("───────────────────────────────────────")
        println("📅 CRÉATION DES TEASERS FUTURS")
        val maxTeaser = 6
        var teasersCreated = 0

        for (offset in 1..maxTeaser) {
            val futureIndex = todayIndex + offset
            if (futureIndex >= categories.size) {
                println("   ⚠️ J+$offset: Plus de catégories disponibles")
                break
            }

            val futureCal = today.clone() as Calendar
            futureCal.add(Calendar.DAY_OF_MONTH, offset)
            val futureDateStr = dateFormat.format(futureCal.time)

            challenges[futureDateStr] = DailyChallenge(
                date = futureDateStr,
                categories = listOf(categories[futureIndex])
            )

            println("   ✅ J+$offset ($futureDateStr): ${categories[futureIndex].name}")
            teasersCreated++
        }

        println("   Total teasers créés: $teasersCreated")
        println("═══════════════════════════════════════")
        println("📦 RÉSUMÉ FINAL")
        println("═══════════════════════════════════════")
        println("🎯 Défis créés        : ${challenges.size}")
        println("🗓️ Dates disponibles  :")
        challenges.keys.sorted().forEach { date ->
            val cat = challenges[date]?.categories?.firstOrNull()?.name ?: "N/A"
            println("   • $date → $cat")
        }
        println("═══════════════════════════════════════")

        return challenges
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        // Cloner pour ne pas modifier les originaux
        val startClean = start.clone() as Calendar
        val endClean = end.clone() as Calendar

        // Reset à minuit pour comparer uniquement les dates
        startClean.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        endClean.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = endClean.timeInMillis - startClean.timeInMillis
        val days = (diffMillis / 86_400_000L).toInt()

        // Si today == startDate : days = 0, on retourne 1 (Jour 1)
        // Si today == startDate + 1 jour : days = 1, on retourne 2 (Jour 2)
        return days + 1
    }

    fun clearCache() {
        println("🗑️ Cache cleared (pas de cache dans cette version)")
    }
}