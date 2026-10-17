package app.brainbox.data.network

import app.brainbox.domain.repository.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class CategoryNetworkService {

    /** [serverTimeMillis] : en-tête HTTP "Date" du serveur, sert à ignorer l'horloge du téléphone. */
    data class Response(val body: String, val serverTimeMillis: Long?)

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

    suspend fun fetchCategories(language: Language): Response {
        return withContext(Dispatchers.IO) {
            val connection = URL(getUrlForLanguage(language)).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.useCaches = false

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP error code: $responseCode")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                Response(body, connection.date.takeIf { it > 0 })
            } finally {
                connection.disconnect()
            }
        }
    }
}
