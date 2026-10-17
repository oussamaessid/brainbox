package app.brainbox.domain.model

data class DailyChallenge(
    val date: String,
    val number: Int,
    val category: Category
)
