package com.lvt.ads.util

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.lvt.ads.callback.AdCallback
import com.lvt.ads.dialog.LoadingAdsDialog
import com.lvt.ads.dialog.ResumeLoadingDialog
import com.lvt.ads.event.AdType
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.event.FirebaseUtil
import java.util.Date

class AppOpenManager private constructor() : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    companion object {
        private const val TAG = "AppOpenManager"
        const val AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/9257395921"
        @Volatile private var INSTANCE: AppOpenManager? = null
        @JvmStatic fun getInstance(): AppOpenManager = INSTANCE ?: synchronized(this) { INSTANCE ?: AppOpenManager().also { INSTANCE = it } }
        @JvmField var checkLoadResume = false
        @JvmField var isShowingAd = false
    }

    private var appResumeAd: AppOpenAd? = null
    private var splashAd: AppOpenAd? = null
    private var appResumeAdId: String? = null
    private var splashAdId: String? = null
    private var currentActivity: Activity? = null
    private var application: Application? = null
    private var appResumeLoadTime = 0L
    private var splashLoadTime = 0L
    private var splashTimeout = 0
    private var isInitialized = false
    private var isAppResumeEnabled = true
    private var isInterstitialShowing = false
    private var enableScreenContentCallback = false
    private var disableAdResumeByClickAction = false
    private val disabledAppOpenList = mutableSetOf<Class<*>>()
    private var splashActivity: Class<*>? = null
    private var fullScreenContentCallback: FullScreenContentCallback? = null
    private var timeoutHandler: Handler? = null
    private var isTimeout = false
    private var dialog: Dialog? = null

    fun init(application: Application, appOpenAdId: String?) {
        isInitialized = true
        disableAdResumeByClickAction = false
        this.application = application
        this.appResumeAdId = appOpenAdId
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun isInitialized() = isInitialized
    fun setInitialized(value: Boolean) { isInitialized = value }
    fun setSplashAdId(value: String?) { splashAdId = value }
    fun setAppResumeAdId(value: String?) { appResumeAdId = value }
    fun setEnableScreenContentCallback(value: Boolean) { enableScreenContentCallback = value }
    fun isInterstitialShowing() = isInterstitialShowing
    fun setInterstitialShowing(value: Boolean) { isInterstitialShowing = value }
    fun disableAdResumeByClickAction() { disableAdResumeByClickAction = true }
    fun setDisableAdResumeByClickAction(value: Boolean) { disableAdResumeByClickAction = value }
    fun isShowingAd() = isShowingAd
    fun disableAppResumeWithActivity(activityClass: Class<*>) { disabledAppOpenList.add(activityClass) }
    fun enableAppResumeWithActivity(activityClass: Class<*>) { disabledAppOpenList.remove(activityClass) }
    fun disableAppResume() { isAppResumeEnabled = false }
    fun enableAppResume() { isAppResumeEnabled = true }
    fun setSplashActivity(activityClass: Class<*>, adId: String?, timeoutInMillis: Int) { splashActivity = activityClass; splashAdId = adId; splashTimeout = timeoutInMillis }
    fun setFullScreenContentCallback(callback: FullScreenContentCallback?) { fullScreenContentCallback = callback }
    fun removeFullScreenContentCallback() { fullScreenContentCallback = null }

    fun fetchAd(isSplash: Boolean) {
        val app = application ?: return
        val adId = if (isSplash) splashAdId else appResumeAdId
        if (adId.isNullOrBlank() || isAdAvailable(isSplash) || checkLoadResume) return
        if (!isSplash) {
            AdmobEvent.openPosition++
            AdmobEvent.logEvent(app, "ad_open_load", Bundle().apply { putInt("ad_open_position", AdmobEvent.openPosition) })
        }
        checkLoadResume = true
        AppOpenAd.load(app, adId, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                checkLoadResume = false
                if (isSplash) {
                    splashAd = ad
                    splashLoadTime = Date().time
                    ad.setOnPaidEventListener { FirebaseUtil.logPaidAdImpression(app, it, ad.adUnitId, AdType.APP_OPEN) }
                } else {
                    AdmobEvent.openPosition++
                    AdmobEvent.logEvent(app, "ad_open_load_success", Bundle().apply { putInt("ad_open_position", AdmobEvent.openPosition) })
                    appResumeAd = ad
                    appResumeLoadTime = Date().time
                }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                checkLoadResume = false
                dismissDialogLoading()
                Log.d(TAG, "load failed: ${error.message}")
                if (!isSplash) AdmobEvent.logEvent(app, "ad_open_load_failed", Bundle().apply { putString("ad_error_message", error.message); putString("internet_status", isNetworkConnected(app).toString()) })
            }
        })
    }

    fun isAdAvailable(isSplash: Boolean): Boolean {
        val loadTime = if (isSplash) splashLoadTime else appResumeLoadTime
        val fresh = Date().time - loadTime < 4 * 60 * 60 * 1000L
        return (if (isSplash) splashAd != null else appResumeAd != null) && fresh
    }

    fun showAdIfAvailable(isSplash: Boolean) {
        val activity = currentActivity ?: run { notifyDismiss(); return }
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) { notifyDismiss(); return }
        if (!isShowingAd && isAdAvailable(isSplash)) {
            if (isSplash) showSplashWithLoading(activity) else showResumeAds(activity)
        } else if (!isSplash) fetchAd(false)
    }

    private fun showSplashWithLoading(activity: Activity) {
        val ad = splashAd ?: return
        val loading = showLoading(activity, resume = true)
        Handler(Looper.getMainLooper()).postDelayed({
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { splashAd = null; isShowingAd = false; notifyDismiss(); fetchAd(true); loading?.safeDismiss() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { fullScreenContentCallback?.onAdFailedToShowFullScreenContent(adError); splashAd = null; isShowingAd = false; loading?.safeDismiss() }
                override fun onAdShowedFullScreenContent() { isShowingAd = true; splashAd = null; AdmobEvent.logEvent(activity, "ad_open_show", Bundle()) }
                override fun onAdClicked() { FirebaseUtil.logClickAdsEvent(activity, splashAdId.orEmpty()); fullScreenContentCallback?.onAdClicked() }
            }
            ad.show(activity)
            loading?.safeDismiss()
        }, 800)
    }

    private fun showResumeAds(activity: Activity) {
        val ad = appResumeAd ?: return
        val loading = showLoading(activity, resume = true)
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { appResumeAd = null; isShowingAd = false; notifyDismiss(); fetchAd(false); loading?.safeDismiss() }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) { appResumeAd = null; isShowingAd = false; fullScreenContentCallback?.onAdFailedToShowFullScreenContent(adError); fetchAd(false); loading?.safeDismiss() }
            override fun onAdShowedFullScreenContent() { isShowingAd = true; appResumeAd = null; AdmobEvent.logEvent(activity, "resume_appopen_view", Bundle()) }
            override fun onAdClicked() { AdmobEvent.logEvent(activity, "resume_appopen_click", Bundle()); FirebaseUtil.logClickAdsEvent(activity, appResumeAdId.orEmpty()); fullScreenContentCallback?.onAdClicked() }
            override fun onAdImpression() { fullScreenContentCallback?.onAdImpression() }
        }
        ad.show(activity)
    }

    fun loadAndShowSplashAds(adId: String?) {
        splashAdId = adId ?: splashAdId
        isTimeout = false
        enableScreenContentCallback = true
        val app = application ?: return
        val unitId = splashAdId ?: return
        AppOpenAd.load(app, unitId, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                timeoutHandler?.removeCallbacksAndMessages(null)
                if (isTimeout) return
                splashAd = ad
                splashLoadTime = Date().time
                ad.setOnPaidEventListener { FirebaseUtil.logPaidAdImpression(app, it, ad.adUnitId, AdType.APP_OPEN) }
                showAdIfAvailable(true)
            }
            override fun onAdFailedToLoad(error: LoadAdError) { if (!isTimeout) notifyDismiss() }
        })
        if (splashTimeout > 0) timeoutHandler = Handler(Looper.getMainLooper()).also { h -> h.postDelayed({ isTimeout = true; notifyDismiss() }, splashTimeout.toLong()) }
    }

    fun showAppOpenSplash(context: Context, callback: AdCallback) {
        val activity = currentActivity ?: context as? Activity
        val ad = splashAd
        if (activity == null || ad == null) { callback.onAdFailedToLoad(null); callback.onNextAction(); return }
        dialog = showLoading(context, resume = false)
        Handler(Looper.getMainLooper()).postDelayed({
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { callback.onNextAction(); callback.onAdClosed(); splashAd = null; isShowingAd = false; dismissDialogLoading() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { callback.onAdFailedToShow(adError); isShowingAd = false; dismissDialogLoading() }
                override fun onAdShowedFullScreenContent() { callback.onAdImpression(); AdmobEvent.logEvent(activity, "splash_appopen_view", Bundle()); isShowingAd = true; splashAd = null }
                override fun onAdClicked() { callback.onAdClicked(); AdmobEvent.logEvent(activity, "splash_appopen_click", Bundle()) }
            }
            ad.show(activity)
        }, 800)
    }

    fun loadOpenAppAdSplash(context: Context, idResumeSplash: String, timeDelay: Long, timeOut: Long, isShowAdIfReady: Boolean, callback: AdCallback) {
        splashAdId = idResumeSplash
        if (!isNetworkConnected(context) || !Admob.isShowAllAds) { Handler(Looper.getMainLooper()).postDelayed({ callback.onAdFailedToLoad(null); callback.onNextAction() }, timeDelay); return }
        val start = System.currentTimeMillis()
        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable { callback.onNextAction(); isShowingAd = false }
        handler.postDelayed(timeoutRunnable, timeOut)
        AppOpenAd.load(context, idResumeSplash, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { handler.removeCallbacks(timeoutRunnable); callback.onAdFailedToLoad(error); callback.onNextAction() }
            override fun onAdLoaded(ad: AppOpenAd) {
                handler.removeCallbacks(timeoutRunnable)
                splashAd = ad
                ad.setOnPaidEventListener { application?.let { app -> FirebaseUtil.logPaidAdImpression(app, it, ad.adUnitId, AdType.APP_OPEN) } }
                if (isShowAdIfReady) Handler(Looper.getMainLooper()).postDelayed({ showAppOpenSplash(context, callback) }, (timeDelay - (System.currentTimeMillis() - start)).coerceAtLeast(0)) else callback.onAdSplashReady()
            }
        })
    }

    fun loadOpenAppAdSplash(context: Context, ids: MutableList<String>?, isShowAdIfReady: Boolean, callback: AdCallback) {
        if (ids.isNullOrEmpty()) { callback.onAdFailedToLoad(null); callback.onNextAction(); return }
        val adId = ids.first()
        AppOpenAd.load(context, adId, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) { ids.removeAt(0); if (ids.isEmpty()) { callback.onAdFailedToLoad(error); callback.onNextAction() } else loadOpenAppAdSplash(context, ids, isShowAdIfReady, callback) }
            override fun onAdLoaded(ad: AppOpenAd) { splashAd = ad; ad.setOnPaidEventListener { application?.let { app -> FirebaseUtil.logPaidAdImpression(app, it, ad.adUnitId, AdType.APP_OPEN) } }; if (isShowAdIfReady) showAppOpenSplash(context, callback) else callback.onAdSplashReady() }
        })
    }

    fun onCheckShowSplashWhenFail(activity: AppCompatActivity, callback: AdCallback, timeDelay: Int) {
        Handler(activity.mainLooper).postDelayed({ if (splashAd != null && !isShowingAd) showAppOpenSplash(activity, callback) }, timeDelay.toLong())
    }

    override fun onStart(owner: LifecycleOwner) {
        val activity = currentActivity ?: return
        if (!isAppResumeEnabled || isInterstitialShowing || disableAdResumeByClickAction) { disableAdResumeByClickAction = false; return }
        if (disabledAppOpenList.any { it.name == activity::class.java.name }) return
        if (splashActivity?.name == activity::class.java.name) { loadAndShowSplashAds(splashAdId); return }
        showAdIfAvailable(false)
    }

    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity; if (activity::class.java.name != AdActivity::class.java.name && splashActivity?.name != activity::class.java.name) fetchAd(false) }
    override fun onActivityDestroyed(activity: Activity) { if (currentActivity === activity) currentActivity = null }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    private fun notifyDismiss() { if (enableScreenContentCallback) fullScreenContentCallback?.onAdDismissedFullScreenContent() }
    private fun showLoading(context: Context, resume: Boolean): Dialog? = runCatching { (if (resume) ResumeLoadingDialog(context) else LoadingAdsDialog(context)).also { it.show() } }.getOrNull()
    private fun dismissDialogLoading() { dialog?.safeDismiss(); dialog = null }
    private fun Dialog.safeDismiss() { runCatching { if (isShowing) dismiss() } }
    private fun isNetworkConnected(context: Context): Boolean = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnected == true
}
