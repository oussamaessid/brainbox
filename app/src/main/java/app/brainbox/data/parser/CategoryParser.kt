package app.brainbox.data.parser

import app.brainbox.domain.model.Category

/**
 * Lit les fichiers de catégories : `{ "Nom": ["a", "b", ...], ... }`, une entrée par jour,
 * dans l'ordre, doublons de noms compris.
 *
 * Volontairement tolérant (les fichiers sont édités à la main) : les virgules manquantes,
 * les espaces insécables et les guillemets échappés ne cassent pas la lecture, et un nom
 * peut contenir n'importe quel caractère (apostrophe, &, espace...).
 */
object CategoryParser {

    fun parse(text: String): List<Category> {
        val result = mutableListOf<Category>()
        var i = 0
        var pendingName: String? = null

        while (i < text.length) {
            when (text[i]) {
                '"' -> {
                    val (value, end) = readString(text, i)
                    i = end
                    pendingName = value
                    continue
                }
                '[' -> {
                    val (items, end) = readArray(text, i)
                    i = end
                    val name = pendingName?.trim()
                    if (!name.isNullOrEmpty() && items.isNotEmpty()) {
                        result += Category(name, items)
                    }
                    pendingName = null
                    continue
                }
            }
            i++
        }
        return result
    }

    /** [start] pointe sur le guillemet ouvrant. Retourne la chaîne et l'index après le guillemet fermant. */
    private fun readString(text: String, start: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var i = start + 1
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\\' && i + 1 < text.length -> {
                    val next = text[i + 1]
                    if (next == 'u' && i + 5 < text.length) {
                        text.substring(i + 2, i + 6).toIntOrNull(16)?.let { sb.append(it.toChar()) }
                        i += 6
                    } else {
                        sb.append(
                            when (next) {
                                'n' -> '\n'
                                't' -> '\t'
                                else -> next
                            }
                        )
                        i += 2
                    }
                    continue
                }
                c == '"' -> return sb.toString() to i + 1
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString() to i
    }

    private fun readArray(text: String, start: Int): Pair<List<String>, Int> {
        val items = mutableListOf<String>()
        var i = start + 1
        while (i < text.length) {
            when (text[i]) {
                '"' -> {
                    val (value, end) = readString(text, i)
                    value.trim().takeIf { it.isNotEmpty() }?.let { items += it }
                    i = end
                    continue
                }
                ']' -> return items to i + 1
            }
            i++
        }
        return items to i
    }
}
