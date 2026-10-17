package app.brainbox

import app.brainbox.data.parser.CategoryParser
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryParserTest {

    @Test
    fun `garde l'ordre et les doublons`() {
        val result = CategoryParser.parse(
            """{ "Fruits": ["Pomme", "Poire"], "Pays": ["France"], "Fruits": ["Kiwi"] }"""
        )
        assertEquals(listOf("Fruits", "Pays", "Fruits"), result.map { it.name })
        assertEquals(listOf("Kiwi"), result[2].items)
    }

    @Test
    fun `tolère une virgule manquante et des espaces insécables`() {
        val json = " {\n  \"Rois\": [\"Louis\", \"Arthur\"]\n  \"Musique\": [\"Gong\"]\n}"
        val result = CategoryParser.parse(json)
        assertEquals(listOf("Rois", "Musique"), result.map { it.name })
        assertEquals(listOf("Louis", "Arthur"), result[0].items)
    }

    @Test
    fun `accepte apostrophes, espaces, arabe et guillemets échappés`() {
        val result = CategoryParser.parse(
            """{ "Jeux d'enfants": ["Cache-cache", "Le \"loup\""], "فواكه": ["تفاح", "موز"] }"""
        )
        assertEquals("Jeux d'enfants", result[0].name)
        assertEquals("Le \"loup\"", result[0].items[1])
        assertEquals("فواكه", result[1].name)
    }

    @Test
    fun `ignore les catégories vides`() {
        assertEquals(0, CategoryParser.parse("""{ "Vide": [] }""").size)
    }
}
