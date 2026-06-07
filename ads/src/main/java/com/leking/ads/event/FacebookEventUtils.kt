package com.leking.ads.event

import android.content.Context
import android.os.Bundle

object FacebookEventUtils {
    fun logEventWithAds(context: Context, params: Bundle) { runCatching { Class.forName("com.facebook.appevents.AppEventsLogger") } }
    fun logPaidAdImpressionValue(context: Context, bundle: Bundle) { runCatching { Class.forName("com.facebook.appevents.AppEventsLogger") } }
    fun logClickAdsEvent(context: Context, bundle: Bundle) { runCatching { Class.forName("com.facebook.appevents.AppEventsLogger") } }
    fun logTotalRevenue001Ad(context: Context, bundle: Bundle) { runCatching { Class.forName("com.facebook.appevents.AppEventsLogger") } }
}
