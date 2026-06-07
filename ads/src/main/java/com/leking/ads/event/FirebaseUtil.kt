package com.lvt.ads.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdValue
import com.google.firebase.analytics.FirebaseAnalytics
import com.lvt.ads.util.AppUtil
import com.lvt.ads.util.SharePreferenceUtils

object FirebaseUtil {
    private const val TAG = "FirebaseUtil"
    fun logClickAdsEvent(context: Context, adUnitId: String) {
        val bundle = Bundle().apply { putString("ad_unit_id", adUnitId) }
        FirebaseAnalyticsUtil.logClickAdsEvent(context, bundle)
        FacebookEventUtils.logClickAdsEvent(context, bundle)
    }
    fun logPaidAdImpression(context: Context, adValue: AdValue, adUnitId: String, adType: AdType) {
        AppsflyerEvent.pushTrackEventAdmob(adValue, adUnitId, adType)
        val revenueMicros = adValue.valueMicros.toFloat()
        val params = Bundle().apply {
            putDouble("valuemicros", revenueMicros.toDouble())
            putString("currency", adValue.currencyCode ?: "USD")
            putInt("precision", adValue.precisionType)
            putString("adunitid", adUnitId)
            putString("network", adType.name)
        }
        FirebaseAnalyticsUtil.logEventWithAds(context, params)
        FacebookEventUtils.logEventWithAds(context, params)
        logPaidAdImpressionValue(context, revenueMicros / 1_000_000.0, adValue.precisionType, adUnitId, adType.name)
        SharePreferenceUtils.updateCurrentTotalRevenueAd(context, revenueMicros)
        AppUtil.currentTotalRevenue001Ad += revenueMicros
        SharePreferenceUtils.updateCurrentTotalRevenue001Ad(context, AppUtil.currentTotalRevenue001Ad)
        logTotalRevenue001Ad(context)
    }
    private fun logPaidAdImpressionValue(context: Context, value: Double, precision: Int, adUnitId: String, network: String) {
        val bundle = Bundle().apply { putDouble("value", value); putString("currency", "USD"); putInt("precision", precision); putString("adunitid", adUnitId); putString("network", network) }
        FirebaseAnalyticsUtil.logPaidAdImpressionValue(context, bundle)
        FacebookEventUtils.logPaidAdImpressionValue(context, bundle)
    }
    fun logTotalRevenue001Ad(context: Context) {
        val revenue = AppUtil.currentTotalRevenue001Ad
        if (revenue / 1_000_000f >= 0.01f) {
            AppUtil.currentTotalRevenue001Ad = 0f
            SharePreferenceUtils.updateCurrentTotalRevenue001Ad(context, 0f)
            val bundle = Bundle().apply { putFloat(FirebaseAnalytics.Param.VALUE, revenue / 1_000_000f); putString(FirebaseAnalytics.Param.CURRENCY, "USD") }
            FirebaseAnalyticsUtil.logTotalRevenue001Ad(context, bundle)
            FacebookEventUtils.logTotalRevenue001Ad(context, bundle)
        }
    }
    fun logTimeLoadAdsSplash(context: Context, timeLoad: Int) { FirebaseAnalytics.getInstance(context).logEvent("event_time_load_ads_splash", Bundle().apply { putString("time_load", timeLoad.toString()) }) }
    fun logTimeLoadShowAdsInter(context: Context, timeLoad: Double) { FirebaseAnalytics.getInstance(context).logEvent("event_time_show_ads_inter", Bundle().apply { putString("time_show", timeLoad.toString()) }) }
}
