package app.brainbox.domain.model

data class DailyChallenge(
    val date: String,
    val categories: List<Category>
)