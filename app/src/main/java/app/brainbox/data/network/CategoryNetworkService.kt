package app.brainbox.data.network

import app.brainbox.domain.repository.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class CategoryNetworkService {

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/oussamaessid/BrainboxData/main"

        private fun getUrlForLanguage(language: Language): String {
            return when (language) {
                Language.ENGLISH -> "$BASE_URL/categories_en.json"
                Language.FRENCH -> "$BASE_URL/categories_fr.json"
                Language.ARABIC -> "$BASE_URL/categories_ar.json"
            }
        }
    }

    suspend fun fetchCategories(language: Language): Map<String, List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(getUrlForLanguage(language))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseJsonWithDuplicates(response)
                } else {
                    throw Exception("HTTP error code: $responseCode")
                }
            } catch (e: Exception) {
                throw Exception("Failed to fetch categories: ${e.message}", e)
            }
        }
    }

    /**
     * 🔥 PARSING MANUEL POUR GÉRER LES DOUBLONS DE CLÉS
     *
     * Problème: JSON standard (org.json.JSONObject ou Gson) écrase les clés dupliquées
     * Solution: Parser manuellement avec regex pour détecter TOUTES les occurrences
     *
     * Comportement:
     * - "Fruits": [...] (1ère occurrence) → clé: "Fruits"
     * - "Fruits": [...] (2ème occurrence) → clé: "Fruits_2"
     * - "Fruits": [...] (3ème occurrence) → clé: "Fruits_3"
     * - etc.
     *
     * Ainsi, chaque ligne du JSON = 1 jour de jeu, dans l'ordre!
     */
    private fun parseJsonWithDuplicates(jsonString: String): Map<String, List<String>> {
        val result = linkedMapOf<String, List<String>>()
        val categoryCounter = mutableMapOf<String, Int>()

        // Regex améliorée pour gérer:
        // - Lettres, chiffres, underscores, tirets
        // - Espaces dans les noms de catégories
        // - Caractères spéciaux comme "Star-anise"
        val regex = """"([\w\s-]+)":\s*\[([^\]]+)\]""".toRegex()

        println("🔍 PARSING MANUEL DU JSON...")
        println("   Recherche de toutes les catégories (incluant doublons)...")

        var dayIndex = 0
        regex.findAll(jsonString).forEach { match ->
            val categoryName = match.groupValues[1].trim()
            val itemsString = match.groupValues[2]

            // Extraire les items individuels
            val items = itemsString
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }

            // Créer une clé unique pour les doublons
            val uniqueKey = if (categoryCounter.containsKey(categoryName)) {
                categoryCounter[categoryName] = categoryCounter[categoryName]!! + 1
                "${categoryName}_${categoryCounter[categoryName]}"
            } else {
                categoryCounter[categoryName] = 1
                categoryName  // Première occurrence: pas de suffixe
            }

            result[uniqueKey] = items
            dayIndex++

            // Afficher les 10 premiers jours pour debug
            if (dayIndex <= 10) {
                val preview = items.take(3).joinToString(", ")
                println("   [$dayIndex] $uniqueKey: $preview... (${items.size} items)")
            }
        }

        if (dayIndex > 10) {
            println("   ... et ${dayIndex - 10} autres catégories")
        }

        println("✅ PARSING TERMINÉ:")
        println("   • ${result.size} catégories chargées")
        println("   • ${categoryCounter.filter { it.value > 1 }.size} catégories avec doublons")

        val duplicates = categoryCounter.filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            println("📊 DOUBLONS DÉTECTÉS:")
            duplicates.forEach { (name, count) ->
                println("   • $name: $count occurrences → renommées ${name}_2, ${name}_3, ...")
            }
        }

        return result
    }
}