package com.lvt.ads.util
import android.content.Context
object Helper {
    private const val FILE_SETTING = "setting.pref"
    private const val FILE_SETTING_ADMOB = "setting_admob.pref"
    private const val IS_FIRST_OPEN = "IS_FIRST_OPEN"
    private const val KEY_FIRST_TIME = "KEY_FIRST_TIME"
    fun getNumClickAdsPerDay(context: Context, idAds: String) = context.getSharedPreferences(FILE_SETTING_ADMOB, Context.MODE_PRIVATE).getInt(idAds, 0)
    fun increaseNumClickAdsPerDay(context: Context, idAds: String) { val p = context.getSharedPreferences(FILE_SETTING_ADMOB, Context.MODE_PRIVATE); p.edit().putInt(idAds, p.getInt(idAds, 0) + 1).apply() }
    fun setupAdmobData(context: Context) {
        val setting = context.getSharedPreferences(FILE_SETTING, Context.MODE_PRIVATE)
        val adPref = context.getSharedPreferences(FILE_SETTING_ADMOB, Context.MODE_PRIVATE)
        if (!setting.getBoolean(IS_FIRST_OPEN, false)) { adPref.edit().putLong(KEY_FIRST_TIME, System.currentTimeMillis()).apply(); setting.edit().putBoolean(IS_FIRST_OPEN, true).apply(); return }
        val first = adPref.getLong(KEY_FIRST_TIME, System.currentTimeMillis())
        if (System.currentTimeMillis() - first >= 24 * 60 * 60 * 1000L) adPref.edit().clear().putLong(KEY_FIRST_TIME, System.currentTimeMillis()).apply()
    }
}
