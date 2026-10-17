package app.brainbox.domain.model

enum class GuessResult {
    CORRECT,
    /** Presque (faute de frappe probable) : pas de vie perdue. */
    CLOSE,
    WRONG
}
