package app.brainbox.data.player

import android.content.Context
import android.util.Log
import app.brainbox.domain.model.LeaderboardEntry
import app.brainbox.domain.model.LeaderboardPage
import app.brainbox.domain.model.LeaderboardPeriod
import app.brainbox.domain.model.PlayerAvatars
import app.brainbox.domain.model.PlayerProfile
import app.brainbox.domain.model.boardCode
import app.brainbox.domain.repository.Language
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

enum class ProfileError { NAME_TAKEN, OFFLINE, INVALID, UNKNOWN }

/**
 * Compte invité créé automatiquement (id + pseudo + avatar), modifiable ensuite.
 * Les victoires sont gardées sur l'appareil puis envoyées aux classements Firestore ;
 * en cas d'échec, l'envoi est retenté au prochain lancement / fin de partie / ouverture du classement.
 */
class PlayerRepository(context: Context, private val cloud: FirestoreClient = FirestoreClient(context)) {

    private val prefs = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
    private val syncLock = Mutex()

    init {
        if (!prefs.contains(KEY_ID)) {
            val language = Language.fromDeviceLocale()
            prefs.edit()
                .putString(KEY_ID, UUID.randomUUID().toString())
                .putString(KEY_NAME, generateName(language))
                .putInt(KEY_AVATAR, Random.nextInt(FIRST_LUDO_AVATAR, PlayerAvatars.COUNT))
                .commit()
        }
    }

    private val _profile = MutableStateFlow(readProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    /** Écran de bienvenue pas encore vu (premier lancement). */
    val needsSetup: Boolean get() = !prefs.getBoolean(KEY_SETUP_DONE, false)

    fun completeSetup() {
        prefs.edit().putBoolean(KEY_SETUP_DONE, true).apply()
    }

    private fun readProfile() = PlayerProfile(
        id = prefs.getString(KEY_ID, "")!!,
        name = prefs.getString(KEY_NAME, "BrainBox")!!,
        avatar = prefs.getInt(KEY_AVATAR, FIRST_LUDO_AVATAR).takeIf { PlayerAvatars.isValid(it) } ?: FIRST_LUDO_AVATAR
    )

    // ==================== PROFIL ====================

    /** null si enregistré, sinon la raison de l'échec (pseudo pris, hors-ligne…). */
    suspend fun saveProfile(name: String, avatar: Int): ProfileError? {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        if (!isValidName(clean) || !PlayerAvatars.isValid(avatar)) return ProfileError.INVALID
        reserveName(clean, avatar)?.let { return it }

        prefs.edit().putString(KEY_NAME, clean).putInt(KEY_AVATAR, avatar).apply()
        _profile.value = readProfile()
        completeSetup()
        sync()
        return null
    }

    /**
     * Réserve le couple pseudo + avatar dans names/{clé} : aucun autre joueur ne peut avoir les deux.
     * Le même pseudo reste libre avec un autre avatar.
     */
    private suspend fun reserveName(name: String, avatar: Int): ProfileError? {
        val key = nameKey(name, avatar)
        if (prefs.getString(KEY_RESERVED, null) == key) return null
        val me = _profile.value.id
        val path = "names/$key"
        fun takenByOther(document: JSONObject?) = document?.optJSONObject("fields")
            ?.optJSONObject("playerId")?.optString("stringValue")?.let { it != me } == true

        try {
            if (takenByOther(cloud.read(path))) return ProfileError.NAME_TAKEN
            cloud.write(path, JSONObject()
                .put("name", string(name))
                .put("avatar", integer(avatar))
                .put("playerId", string(me)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Course perdue contre un autre joueur entre la lecture et l'écriture.
            if (e is FirestoreException && e.status == "PERMISSION_DENIED" &&
                runCatching { takenByOther(cloud.read(path)) }.getOrDefault(false)
            ) return ProfileError.NAME_TAKEN
            return if (e is IOException) ProfileError.OFFLINE else ProfileError.UNKNOWN
        }
        prefs.edit().putString(KEY_RESERVED, key).apply()
        return null
    }

    // ==================== RÉSULTATS ====================

    /** Enregistre une victoire du défi n°[dayNumber] (points de 10 à 100). */
    fun recordWin(language: Language, dayNumber: Int, points: Int) {
        val key = "$KEY_WIN_PREFIX${language.boardCode()}_$dayNumber"
        if (!prefs.contains(key)) prefs.edit().putInt(key, points.coerceIn(10, 100)).apply()
    }

    private data class Win(val code: String, val dayNumber: Int, val points: Int)

    private fun wins(): List<Win> = prefs.all.mapNotNull { (key, value) ->
        if (!key.startsWith(KEY_WIN_PREFIX)) return@mapNotNull null
        val (code, day) = key.removePrefix(KEY_WIN_PREFIX).split("_").takeIf { it.size == 2 } ?: return@mapNotNull null
        Win(code, day.toIntOrNull() ?: return@mapNotNull null, value as? Int ?: return@mapNotNull null)
    }

    /** board -> (score, victoires), pour chaque période jouée, par langue et toutes langues. */
    private fun boards(): Map<String, Pair<Int, Int>> {
        val totals = linkedMapOf<String, Pair<Int, Int>>()
        for (win in wins()) {
            val language = Language.entries.firstOrNull { it.boardCode() == win.code } ?: continue
            for (period in LeaderboardPeriod.entries) for (scope in listOf(language, null)) {
                val board = period.boardId(scope, win.dayNumber)
                val (score, count) = totals[board] ?: (0 to 0)
                totals[board] = (score + win.points) to (count + 1)
            }
        }
        return totals
    }

    /** Envoie les classements modifiés. Silencieux : un échec est retenté plus tard. */
    suspend fun sync() = syncLock.withLock {
        val profile = _profile.value
        for ((board, total) in boards()) {
            val (score, wins) = total
            val signature = "$score:$wins:${profile.name}:${profile.avatar}"
            if (prefs.getString("$KEY_SYNCED_PREFIX$board", null) == signature) continue
            try {
                cloud.write("leaderboards/$board/entries/${profile.id}", JSONObject()
                    .put("name", string(profile.name))
                    .put("avatar", integer(profile.avatar))
                    .put("score", integer(score))
                    .put("wins", integer(wins)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Sync impossible ($board) : ${e.message}")
                if (e is IOException) return@withLock // hors-ligne : inutile d'essayer les autres
                continue
            }
            prefs.edit().putString("$KEY_SYNCED_PREFIX$board", signature).apply()
        }
    }

    // ==================== CLASSEMENT ====================

    /** Top [TOP_SIZE] + ma position. Hors-ligne : dernière page en cache, sinon exception. */
    suspend fun leaderboard(period: LeaderboardPeriod, scope: Language?, todayNumber: Int): LeaderboardPage {
        val board = period.boardId(scope, todayNumber)
        val parent = "leaderboards/$board"
        val me = _profile.value.id
        return try {
            sync()
            val top = cloud.query(parent, entriesQuery()
                .put("orderBy", JSONArray().put(JSONObject()
                    .put("field", JSONObject().put("fieldPath", "score")).put("direction", "DESCENDING")))
                .put("limit", TOP_SIZE)
            ).map(::toEntry)
            // Classement "compétition" : les ex æquo partagent le même rang.
            val ranked = top.mapIndexed { i, entry ->
                entry.copy(rank = top.indexOfFirst { it.score == entry.score }.takeIf { it >= 0 }?.plus(1) ?: (i + 1))
            }
            val mine = ranked.firstOrNull { it.uid == me } ?: cloud.read("$parent/entries/$me")?.let { document ->
                val entry = toEntry(document)
                val ahead = cloud.count(parent, entriesQuery().put("where", JSONObject().put("fieldFilter", JSONObject()
                    .put("field", JSONObject().put("fieldPath", "score")).put("op", "GREATER_THAN")
                    .put("value", integer(entry.score)))))
                entry.copy(rank = ahead.toInt() + 1)
            }
            LeaderboardPage(ranked, mine).also { prefs.edit().putString("$KEY_CACHE_PREFIX$board", encode(it)).apply() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            prefs.getString("$KEY_CACHE_PREFIX$board", null)?.let { runCatching { decode(it) }.getOrNull() } ?: throw e
        }
    }

    private fun entriesQuery() = JSONObject().put("from", JSONArray().put(JSONObject().put("collectionId", "entries")))

    private fun toEntry(document: JSONObject): LeaderboardEntry {
        val fields = document.getJSONObject("fields")
        return LeaderboardEntry(
            uid = document.getString("name").substringAfterLast('/'),
            name = fields.getJSONObject("name").getString("stringValue"),
            avatar = fields.optJSONObject("avatar")?.optString("integerValue")?.toIntOrNull() ?: FIRST_LUDO_AVATAR,
            score = fields.getJSONObject("score").getString("integerValue").toInt(),
            wins = fields.getJSONObject("wins").getString("integerValue").toInt(),
            rank = 0
        )
    }

    private fun encode(page: LeaderboardPage): String {
        fun json(e: LeaderboardEntry) = JSONObject().put("uid", e.uid).put("name", e.name).put("avatar", e.avatar)
            .put("score", e.score).put("wins", e.wins).put("rank", e.rank)
        return JSONObject()
            .put("entries", JSONArray().apply { page.entries.forEach { put(json(it)) } })
            .put("me", page.me?.let(::json) ?: JSONObject.NULL)
            .toString()
    }

    private fun decode(raw: String): LeaderboardPage {
        fun entry(o: JSONObject) = LeaderboardEntry(o.getString("uid"), o.getString("name"), o.getInt("avatar"),
            o.getInt("score"), o.getInt("wins"), o.getInt("rank"))
        val json = JSONObject(raw)
        val entries = json.getJSONArray("entries").let { a -> (0 until a.length()).map { entry(a.getJSONObject(it)) } }
        return LeaderboardPage(entries, json.optJSONObject("me")?.let(::entry), cached = true)
    }

    companion object {
        private const val TAG = "PlayerRepository"
        private const val KEY_ID = "player_id"
        private const val KEY_NAME = "player_name"
        private const val KEY_AVATAR = "player_avatar"
        private const val KEY_SETUP_DONE = "profile_setup_done"
        private const val KEY_RESERVED = "reserved_name"
        private const val KEY_WIN_PREFIX = "win_"
        private const val KEY_SYNCED_PREFIX = "synced_"
        private const val KEY_CACHE_PREFIX = "cache_"
        private const val FIRST_LUDO_AVATAR = 8
        const val TOP_SIZE = 50

        fun isValidName(name: String) = name.trim().length in 2..24 && name.none { it.isISOControl() }

        private fun string(value: String) = JSONObject().put("stringValue", value)
        private fun integer(value: Int) = JSONObject().put("integerValue", value.toString())

        /**
         * Id Firestore commun à toutes les écritures d'un pseudo avec le même avatar :
         * "Alice", " ALICE " et "alice" entrent en collision, "Alice" avec un autre avatar non.
         */
        fun nameKey(name: String, avatar: Int): String =
            Normalizer.normalize(name.trim().replace(Regex("\\s+"), " "), Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) } + "_$avatar"

        /** Pseudo automatique : "RenardFuté42", "CleverFox42", "صقر ذكي 42". */
        fun generateName(language: Language, random: Random = Random): String {
            val number = random.nextInt(10, 100)
            return when (language) {
                Language.FRENCH -> listOf("Renard", "Hibou", "Panda", "Tigre", "Lynx", "Dauphin", "Faucon", "Koala").random(random) +
                        listOf("Malin", "Futé", "Rapide", "Curieux", "Brillant", "Sage", "Agile", "Vif").random(random) + number
                Language.ENGLISH -> listOf("Clever", "Swift", "Bright", "Witty", "Wise", "Smart", "Quick", "Bold").random(random) +
                        listOf("Fox", "Owl", "Panda", "Tiger", "Lynx", "Dolphin", "Falcon", "Koala").random(random) + number
                Language.ARABIC -> listOf("ثعلب", "نمر", "صقر", "فهد", "أسد", "دلفين", "ذئب", "نسر").random(random) + " " +
                        listOf("ذكي", "سريع", "لامع", "حكيم", "شجاع", "فطن", "نبيه", "بارع").random(random) + " " + number
            }
        }
    }
}
