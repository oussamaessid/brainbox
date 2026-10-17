package app.brainbox.domain.usecase

import app.brainbox.domain.model.GameState
import app.brainbox.domain.repository.Language

/** Texte de partage façon Wordle : ne dévoile pas la réponse. */
class BuildShareTextUseCase {

    operator fun invoke(
        language: Language,
        challengeNumber: Int,
        isWin: Boolean,
        attempts: Int,
        hintsUsed: Int,
        streak: Int
    ): String {
        val max = GameState.MAX_LIVES
        val wrong = if (isWin) attempts - 1 else max
        val grid = "🟥".repeat(wrong) + (if (isWin) "🟩" else "") +
                "⬛".repeat((max - wrong - if (isWin) 1 else 0).coerceAtLeast(0))
        val result = if (isWin) "$attempts/$max" else "X/$max"

        val flag = when (language) {
            Language.FRENCH -> "🇫🇷"
            Language.ENGLISH -> "🇬🇧"
            Language.ARABIC -> "🇸🇦"
        }
        val (hintLabel, streakLabel, cta) = when (language) {
            Language.FRENCH -> Triple("indice", "Série", "Saurez-vous trouver la catégorie ?")
            Language.ENGLISH -> Triple("hint", "Streak", "Can you guess the category?")
            Language.ARABIC -> Triple("تلميح", "سلسلة", "هل يمكنك تخمين الفئة؟")
        }

        return buildString {
            appendLine("🧠 BrainBox #$challengeNumber $flag")
            appendLine("$grid  $result")
            if (hintsUsed > 0) appendLine("💡 $hintsUsed $hintLabel")
            if (streak > 1) appendLine("🔥 $streakLabel : $streak")
            appendLine(cta)
            append(STORE_URL)
        }
    }

    companion object {
        const val STORE_URL = "https://play.google.com/store/apps/details?id=app.brainbox"
    }
}
