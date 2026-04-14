package app.brainbox.domain.usecase

class ValidateGuessUseCase {
    operator fun invoke(guess: String, categoryName: String): Boolean {
        val cleanCategory = categoryName
            .trim()
            .replace(Regex("_\\d+$"), "")
            .lowercase()
            .normalize()

        val cleanGuess = guess
            .trim()
            .lowercase()
            .normalize()

        println("🔍 Compare: '$cleanGuess' vs '$cleanCategory'")
        return cleanGuess == cleanCategory
    }

    private fun String.normalize(): String {
        return java.text.Normalizer
            .normalize(this, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}