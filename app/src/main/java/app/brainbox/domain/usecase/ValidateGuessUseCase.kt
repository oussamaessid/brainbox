package app.brainbox.domain.usecase

import app.brainbox.domain.model.GuessResult
import app.brainbox.domain.repository.Language
import java.text.Normalizer

class ValidateGuessUseCase {

    operator fun invoke(guess: String, categoryName: String, language: Language): GuessResult {
        val cleanGuess = normalize(guess, language)
        val cleanCategory = normalize(categoryName, language)
        if (cleanGuess.isEmpty()) return GuessResult.WRONG
        if (cleanGuess == cleanCategory) return GuessResult.CORRECT

        val distance = levenshtein(cleanGuess, cleanCategory)
        val length = cleanCategory.length
        return when {
            // Une seule faute de frappe sur un mot assez long : on accepte.
            distance == 1 && length >= 6 -> GuessResult.CORRECT
            distance <= 2 && length >= 5 -> GuessResult.CLOSE
            distance == 1 && length >= 3 -> GuessResult.CLOSE
            else -> GuessResult.WRONG
        }
    }

    internal fun normalize(text: String, language: Language): String {
        var s = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "") // accents latins + harakat arabes
            .replace(Regex("[\\s'’_\\-.]"), "")

        if (language == Language.ARABIC) {
            // Le clavier n'a pas toutes les formes de hamza / alif : on les unifie.
            s = s.replace(Regex("[أإآٱ]"), "ا")
                .replace('ى', 'ي')
                .replace('ئ', 'ي')
                .replace('ؤ', 'و')
                .replace('ة', 'ه')
                .replace("ء", "")
                .replace("ـ", "")
        } else if (s.length > 3 && (s.endsWith("s") || s.endsWith("x"))) {
            // Singulier / pluriel : "Fruit" == "Fruits", "Jeu" == "Jeux"
            s = s.dropLast(1)
        }
        return s
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}
