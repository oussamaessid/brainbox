package app.brainbox.data.network

import app.brainbox.domain.repository.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseJsonResponse(response)
                } else {
                    throw Exception("HTTP error code: $responseCode")
                }
            } catch (e: Exception) {
                throw Exception("Failed to fetch categories: ${e.message}", e)
            }
        }
    }

    private fun parseJsonResponse(jsonString: String): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        val jsonObject = JSONObject(jsonString)

        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val jsonArray = jsonObject.getJSONArray(key)
            val items = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                items.add(jsonArray.getString(i))
            }

            result[key] = items
        }

        return result
    }
}