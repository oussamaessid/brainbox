package app.brainbox.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "brainbox_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    /**
     * Check if this is the first time the app is launched
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Mark that the app has been launched (tutorial has been shown)
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * Reset first launch flag (useful for testing)
     */
    fun resetFirstLaunch() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }
}