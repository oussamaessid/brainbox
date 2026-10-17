package app.brainbox.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Calendrier du jeu : jour 1 = 28 janvier 2026, un défi par jour (heure locale).
 * Calculé à partir des champs année/mois/jour, donc insensible aux changements d'heure
 * (l'ancien calcul en millisecondes perdait un jour pendant l'heure d'été).
 */
class GameCalendar(private val timeZone: TimeZone = TimeZone.getDefault()) {

    /** Numéro du défi (1 = premier jour). Peut être < 1 avant le lancement. */
    fun dayNumber(nowMillis: Long): Int = (epochDay(nowMillis) - START_EPOCH_DAY + 1).toInt()

    /** Format historique des clés de sauvegarde. Locale.US : toujours des chiffres latins. */
    fun formatDate(nowMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = this@GameCalendar.timeZone }
            .format(Date(nowMillis))

    fun millisUntilNextDay(nowMillis: Long): Long {
        val next = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return next.timeInMillis - nowMillis
    }

    private fun epochDay(nowMillis: Long): Long {
        val local = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        return utc.timeInMillis / MILLIS_PER_DAY
    }

    companion object {
        /** Jour (depuis le 01/01/1970) correspondant au défi n°[dayNumber]. */
        fun epochDayOf(dayNumber: Int): Long = START_EPOCH_DAY + dayNumber - 1

        private const val MILLIS_PER_DAY = 86_400_000L
        // 28/01/2026 en jours depuis le 01/01/1970
        private const val START_EPOCH_DAY = 20481L
    }
}
