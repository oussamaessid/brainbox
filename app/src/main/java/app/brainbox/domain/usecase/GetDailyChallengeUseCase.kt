package app.brainbox.domain.usecase

import app.brainbox.domain.model.DailyChallenge
import app.brainbox.domain.repository.GameRepository
import app.brainbox.domain.repository.Language

class GetDailyChallengeUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(language: Language, date: String): DailyChallenge? {
        println("═══════════════════════════════════════")
        println("🔍 GetDailyChallengeUseCase")
        println("   Langue: $language")
        println("   Date recherchée: $date")

        val challenges = repository.getDailyChallenges(language)

        println("📊 Résultat getDailyChallenges:")
        println("   Nombre total: ${challenges.size}")
        println("   Dates disponibles: ${challenges.keys.sorted()}")

        val result = challenges[date]

        if (result == null) {
            println("❌ Aucun challenge trouvé pour $date")
            println("   Vérifiez que la date est au bon format: dd/MM/yyyy")
        } else {
            val category = result.categories.firstOrNull()
            println("✅ Challenge trouvé!")
            println("   Date: ${result.date}")
            println("   Catégorie: ${category?.name ?: "N/A"}")
            println("   Items: ${category?.items?.joinToString(", ") ?: "N/A"}")
        }
        println("═══════════════════════════════════════")

        return result
    }
}