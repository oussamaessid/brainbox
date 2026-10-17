package app.brainbox.data.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

class FirestoreException(message: String, val status: String) : Exception(message)

/**
 * Client REST Firestore sans Firebase Auth ni SDK (plan gratuit Spark), comme dans Equal To.
 * Les accès sont contrôlés par firebase/firestore.rules.
 */
class FirestoreClient(context: Context) {

    private val packageName = context.packageName
    private val certificate by lazy { runCatching { signingSha1(context) }.getOrNull() }
    private val database = "projects/$PROJECT_ID/databases/(default)"

    fun documentName(path: String) = "$database/documents/$path"

    /** Remplace chaque document et horodate updatedAt avec l'heure du serveur. */
    suspend fun write(path: String, fields: JSONObject) = withContext(Dispatchers.IO) {
        val write = JSONObject()
            .put("update", JSONObject().put("name", documentName(path)).put("fields", fields))
            .put("updateTransforms", JSONArray().put(JSONObject()
                .put("fieldPath", "updatedAt").put("setToServerValue", "REQUEST_TIME")))
        request("$BASE/$database/documents:commit", JSONObject().put("writes", JSONArray().put(write)).toString())
        Unit
    }

    /** Un document, ou null s'il n'existe pas. */
    suspend fun read(path: String): JSONObject? = withContext(Dispatchers.IO) {
        request("$BASE/${documentName(path)}", null)?.let { JSONObject(it) }
    }

    suspend fun query(parentPath: String, structuredQuery: JSONObject): List<JSONObject> = withContext(Dispatchers.IO) {
        val raw = request("$BASE/${documentName(parentPath)}:runQuery",
            JSONObject().put("structuredQuery", structuredQuery).toString())!!
        val rows = JSONArray(raw)
        (0 until rows.length()).mapNotNull { rows.getJSONObject(it).optJSONObject("document") }
    }

    /** Agrégation COUNT : facturée une lecture pour 1000 documents. */
    suspend fun count(parentPath: String, structuredQuery: JSONObject): Long = withContext(Dispatchers.IO) {
        val body = JSONObject().put("structuredAggregationQuery", JSONObject()
            .put("structuredQuery", structuredQuery)
            .put("aggregations", JSONArray().put(JSONObject().put("alias", "n").put("count", JSONObject()))))
        val raw = request("$BASE/${documentName(parentPath)}:runAggregationQuery", body.toString())!!
        JSONArray(raw).getJSONObject(0).getJSONObject("result").getJSONObject("aggregateFields")
            .getJSONObject("n").getString("integerValue").toLong()
    }

    /** body = null : GET, et un document absent renvoie null au lieu d'une erreur. */
    private fun request(url: String, body: String?): String? {
        val connection = URL("$url?key=${URLEncoder.encode(API_KEY, "UTF-8")}").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            // Permet de restreindre la clé API à l'app Android dans la console Google Cloud.
            connection.setRequestProperty("X-Android-Package", packageName)
            certificate?.let { connection.setRequestProperty("X-Android-Cert", it) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (body == null && code == 404) return null
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val error = (runCatching { JSONArray(raw).getJSONObject(0) }.getOrNull()
                    ?: runCatching { JSONObject(raw) }.getOrNull())?.optJSONObject("error")
                val status = error?.optString("status")?.takeIf { it.isNotBlank() } ?: "UNAVAILABLE"
                Log.d(TAG, "HTTP $code $status")
                throw FirestoreException(error?.optString("message")?.take(180) ?: "HTTP_$code", status)
            }
            return raw
        } finally {
            connection.disconnect()
        }
    }

    private fun signingSha1(context: Context): String? {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        else pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        val signature = signatures?.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-1").digest(signature.toByteArray()).joinToString("") { "%02X".format(it) }
    }

    companion object {
        private const val TAG = "Firestore"
        private const val BASE = "https://firestore.googleapis.com/v1"
        // Valeurs de app/google-services.json (projet brainbox-daily) ; publiques dans tout APK de toute façon.
        const val PROJECT_ID = "brainbox-daily"
        private const val API_KEY = "AIzaSyD93zeP1FBlU5mXPBPO-CMo2M0shT1wu50"
    }
}
