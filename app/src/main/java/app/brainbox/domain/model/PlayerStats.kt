package app.brainbox.domain.model

data class PlayerStats(
    val played: Int = 0,
    val wins: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    /** Numéro du dernier défi gagné (pour calculer la série). */
    val lastWinDay: Int = 0,
    /** distribution[i] = victoires en i+1 essais. */
    val distribution: List<Int> = List(GameState.MAX_LIVES) { 0 }
) {
    val winRate: Int get() = if (played == 0) 0 else (wins * 100) / played

    /** Série affichée aujourd'hui : elle est perdue si le défi d'hier n'a pas été gagné. */
    fun streakOn(dayNumber: Int): Int =
        if (lastWinDay >= dayNumber - 1) currentStreak else 0

    fun record(dayNumber: Int, isWin: Boolean, attempts: Int): PlayerStats {
        if (!isWin) return copy(played = played + 1, currentStreak = 0)

        val streak = if (lastWinDay == dayNumber - 1) currentStreak + 1 else 1
        val index = (attempts - 1).coerceIn(0, distribution.size - 1)
        return copy(
            played = played + 1,
            wins = wins + 1,
            currentStreak = streak,
            maxStreak = maxOf(maxStreak, streak),
            lastWinDay = dayNumber,
            distribution = distribution.mapIndexed { i, n -> if (i == index) n + 1 else n }
        )
    }
}
