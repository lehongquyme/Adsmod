package com.lvt.ads.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

class AdsConsentManager(private val activity: Activity) {
    fun interface UMPResultListener { fun onCheckUMPSuccess(result: Boolean) }
    private val called = AtomicBoolean(false)

    fun requestUMP(listener: UMPResultListener) = requestUMP(false, "", false, listener)

    fun requestUMP(enableDebug: Boolean, testDevice: String, resetData: Boolean, listener: UMPResultListener) {
        val paramsBuilder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)
        if (enableDebug) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .apply { if (testDevice.isNotBlank()) addTestDeviceHashedId(testDevice) }
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        if (resetData) consentInformation.reset()
        consentInformation.requestConsentInfoUpdate(activity, paramsBuilder.build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                error?.let { Log.e(TAG, it.message) }
                if (!called.getAndSet(true)) listener.onCheckUMPSuccess(getConsentResult(activity))
            }
        }, { error ->
            Log.e(TAG, error.message)
            if (!called.getAndSet(true)) listener.onCheckUMPSuccess(getConsentResult(activity))
        })
        if (consentInformation.canRequestAds() && !called.getAndSet(true)) listener.onCheckUMPSuccess(getConsentResult(activity))
    }

    fun showPrivacyOption(activity: Activity, listener: UMPResultListener) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { listener.onCheckUMPSuccess(getConsentResult(activity)) }
    }

    companion object {
        private const val TAG = "AdsConsentManager"
        @JvmStatic fun getConsentResult(context: Context): Boolean {
            val consent = context.getSharedPreferences(context.packageName + "_preferences", 0).getString("IABTCF_PurposeConsents", "").orEmpty()
            return consent.isEmpty() || consent.firstOrNull() == '1'
        }
    }
}
