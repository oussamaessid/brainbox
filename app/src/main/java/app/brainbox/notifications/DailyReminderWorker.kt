package app.brainbox.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.brainbox.MainActivity
import app.brainbox.R
import app.brainbox.data.repository.TrustedClock
import app.brainbox.domain.model.GameCalendar
import app.brainbox.domain.repository.Language
import app.brainbox.utils.PreferencesManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Rappel quotidien vers 18 h, seulement si le défi du jour n'a pas encore été joué
 * dans la dernière langue utilisée.
 */
class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        val language = prefs.getLastLanguage() ?: return Result.success()
        val clock = TrustedClock(applicationContext)
        val calendar = GameCalendar()
        val now = clock.now()

        if (calendar.dayNumber(now) < 1) return Result.success()
        if (prefs.isGameCompleted(language, calendar.formatDate(now))) return Result.success()

        val streak = prefs.getStats(language).streakOn(calendar.dayNumber(now))
        show(applicationContext, language, streak)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_reminder"
        private const val CHANNEL_ID = "daily_challenge"
        private const val NOTIFICATION_ID = 1001
        private const val REMINDER_HOUR = 18

        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun show(context: Context, language: Language, streak: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return

            val (channelName, title, body) = texts(language, streak)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_brainbox)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        private fun texts(language: Language, streak: Int): Triple<String, String, String> = when (language) {
            Language.FRENCH -> Triple(
                "Défi du jour",
                "🧠 Le défi du jour vous attend !",
                if (streak > 0) "Ne perdez pas votre série de $streak jour(s) 🔥" else "Trouverez-vous la catégorie aujourd'hui ?"
            )
            Language.ENGLISH -> Triple(
                "Daily challenge",
                "🧠 Today's challenge is waiting!",
                if (streak > 0) "Don't lose your $streak-day streak 🔥" else "Can you find today's category?"
            )
            Language.ARABIC -> Triple(
                "تحدي اليوم",
                "🧠 تحدي اليوم بانتظارك!",
                if (streak > 0) "لا تخسر سلسلتك من $streak يوم 🔥" else "هل ستجد فئة اليوم؟"
            )
        }
    }
}
