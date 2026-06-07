package com.leking.ads.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AdmobEvent {
    var openPosition: Int = 0
    private const val TAG = "AdmobEvent"
    @JvmStatic fun logEvent(context: Context, nameEvent: String, params: Bundle = Bundle()) {
        Log.d(TAG, nameEvent)
        FirebaseAnalytics.getInstance(context).logEvent(nameEvent, params)
    }
}
