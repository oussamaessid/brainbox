package app.brainbox

import app.brainbox.domain.model.GuessResult
import app.brainbox.domain.repository.Language
import app.brainbox.domain.usecase.ValidateGuessUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateGuessUseCaseTest {

    private val validate = ValidateGuessUseCase()

    @Test
    fun `réponse exacte, casse et accents ignorés`() {
        assertEquals(GuessResult.CORRECT, validate("PLANÈTES", "Planetes", Language.FRENCH))
        assertEquals(GuessResult.CORRECT, validate("  fruits ", "Fruits", Language.FRENCH))
    }

    @Test
    fun `singulier et pluriel acceptés`() {
        assertEquals(GuessResult.CORRECT, validate("FRUIT", "Fruits", Language.FRENCH))
        assertEquals(GuessResult.CORRECT, validate("ANIMAL", "Animals", Language.ENGLISH))
    }

    @Test
    fun `une faute de frappe sur un mot long est acceptée`() {
        assertEquals(GuessResult.CORRECT, validate("LEGUMSS", "Legumes", Language.FRENCH))
    }

    @Test
    fun `presque juste ne coûte pas de vie`() {
        assertEquals(GuessResult.CLOSE, validate("METRS", "Metiers", Language.FRENCH))
        assertEquals(GuessResult.CLOSE, validate("PAIS", "Pays", Language.FRENCH))
    }

    @Test
    fun `mauvaise réponse`() {
        assertEquals(GuessResult.WRONG, validate("COULEURS", "Fruits", Language.FRENCH))
        assertEquals(GuessResult.WRONG, validate("", "Fruits", Language.FRENCH))
    }

    @Test
    fun `arabe - formes de hamza et alif unifiées`() {
        // Le clavier n'a pas "ى" ni "ء" : la réponse doit rester possible.
        assertEquals(GuessResult.CORRECT, validate("الوان", "ألوان", Language.ARABIC))
        assertEquals(GuessResult.CORRECT, validate("حلوي", "حلوى", Language.ARABIC))
        assertEquals(GuessResult.CORRECT, validate("فواكه", "فواكة", Language.ARABIC))
    }
}
