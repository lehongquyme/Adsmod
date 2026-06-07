package com.lvt.ads.util

import android.app.Application
import android.util.Log

abstract class AdsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppUtil.BUILD_DEBUG = buildDebug()
        Log.i("AdsApplication", "run debug: ${AppUtil.BUILD_DEBUG}")
        Admob.getInstance().initAdmob(this, getListTestDeviceId())
        if (enableAdsResume()) AppOpenManager.getInstance().init(this, getResumeAdId())
    }
    abstract fun enableAdsResume(): Boolean
    abstract fun getListTestDeviceId(): List<String>?
    abstract fun getResumeAdId(): String
    abstract fun buildDebug(): Boolean
}
