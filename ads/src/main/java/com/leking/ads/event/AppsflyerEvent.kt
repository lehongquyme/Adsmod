package com.lvt.ads.event

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.AdValue

class AppsflyerEvent private constructor() {
    companion object {
        private const val TAG = "AppsflyerEvent"
        @JvmStatic val instance: AppsflyerEvent by lazy { AppsflyerEvent() }
        @JvmStatic var enableTrackingRevenue: Boolean = false
        @JvmStatic fun pushTrackEventAdmob(adValue: AdValue, idAd: String, adType: AdType) {
            Log.d(TAG, "paid ${adValue.valueMicros / 1_000_000.0} $idAd $adType")
        }
    }
    fun init(context: Application, devKey: String, enableTrackingRevenue: Boolean) { Companion.enableTrackingRevenue = enableTrackingRevenue }
    fun initDebug(context: Application, devKey: String, enableDebugLog: Boolean) {}
}
