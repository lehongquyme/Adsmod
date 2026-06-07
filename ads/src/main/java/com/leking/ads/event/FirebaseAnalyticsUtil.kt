package com.leking.ads.event

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object FirebaseAnalyticsUtil {
    fun logEventWithAds(context: Context, params: Bundle) = FirebaseAnalytics.getInstance(context).logEvent("admob_paid_ad_impression", params)
    fun logPaidAdImpressionValue(context: Context, bundle: Bundle) = FirebaseAnalytics.getInstance(context).logEvent("admob_paid_ad_impression_value", bundle)
    fun logClickAdsEvent(context: Context, bundle: Bundle) = FirebaseAnalytics.getInstance(context).logEvent("admob_event_user_click_ads", bundle)
    fun logTotalRevenue001Ad(context: Context, bundle: Bundle) = FirebaseAnalytics.getInstance(context).logEvent("admob_daily_ads_revenue", bundle)
}
