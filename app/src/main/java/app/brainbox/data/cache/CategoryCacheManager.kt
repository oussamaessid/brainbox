package app.brainbox.data.cache

import android.content.Context
import app.brainbox.domain.repository.Language
import java.io.File

/** Garde la dernière version téléchargée des catégories pour pouvoir jouer hors-ligne. */
class CategoryCacheManager(context: Context) {

    private val dir = File(context.filesDir, "categories")

    fun read(language: Language): String? =
        file(language).takeIf { it.exists() }?.runCatching { readText() }?.getOrNull()

    fun write(language: Language, content: String) {
        runCatching {
            dir.mkdirs()
            val tmp = File(dir, "${language.name}.tmp")
            tmp.writeText(content)
            tmp.renameTo(file(language))
        }
    }

    fun clearCache() {
        dir.deleteRecursively()
    }

    private fun file(language: Language) = File(dir, "${language.name}.json")
}
