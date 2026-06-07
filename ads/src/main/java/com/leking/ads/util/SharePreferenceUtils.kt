package com.lvt.ads.util
import android.content.Context
object SharePreferenceUtils {
    private const val PREF_NAME = "ad_pref"
    private const val KEY_CURRENT_TOTAL_REVENUE_001_AD = "KEY_CURRENT_TOTAL_REVENUE_001_AD"
    private const val KEY_CURRENT_TOTAL_REVENUE_AD = "KEY_CURRENT_TOTAL_REVENUE_AD"
    fun updateCurrentTotalRevenue001Ad(context: Context, revenue: Float) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_001_AD, revenue).apply()
    fun updateCurrentTotalRevenueAd(context: Context, revenueMicros: Float) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_AD, pref.getFloat(KEY_CURRENT_TOTAL_REVENUE_AD, 0f) + revenueMicros / 1_000_000f).apply()
    }
}
