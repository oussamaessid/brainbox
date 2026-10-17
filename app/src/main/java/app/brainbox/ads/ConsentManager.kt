package app.brainbox.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import app.brainbox.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Consentement RGPD via Google UMP. Obligatoire pour diffuser des pubs AdMob dans l'EEE/UK.
 * Le formulaire s'affiche seulement si un message "RGPD" est configuré dans la console AdMob
 * (Confidentialité et messages) et si l'utilisateur est concerné.
 */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Met à jour le consentement et affiche le formulaire si nécessaire, puis appelle [onDone]. */
    fun gather(activity: Activity, onDone: () -> Unit) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                // Simule un utilisateur européen sur émulateur pour tester le formulaire.
                setConsentDebugSettings(
                    ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .build()
                )
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    error?.let { Log.w(TAG, "Formulaire de consentement : ${it.message}") }
                    onDone()
                }
            },
            { error ->
                Log.w(TAG, "Mise à jour du consentement impossible : ${error.message}")
                onDone()
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            error?.let { Log.w(TAG, "Options de confidentialité : ${it.message}") }
        }
    }

    companion object {
        private const val TAG = "ConsentManager"
    }
}
