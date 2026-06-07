package com.leking.ads.util

import androidx.multidex.MultiDexApplication

abstract class AdsMultiDexApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        AppUtil.BUILD_DEBUG = buildDebug()
        Admob.getInstance().initAdmob(this, getListTestDeviceId())
        if (enableAdsResume()) AppOpenManager.getInstance().init(this, getOpenAppAdId())
    }
    abstract fun enableAdsResume(): Boolean
    abstract fun getListTestDeviceId(): List<String>?
    abstract fun getOpenAppAdId(): String
    open fun buildDebug(): Boolean = false
}
