package com.leking.ads.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.leking.ads.R
import com.leking.ads.callback.BannerCallBack
import com.leking.ads.callback.InterCallback
import com.leking.ads.callback.NativeCallback
import com.leking.ads.callback.RewardCallback
import com.leking.ads.dialog.LoadingAdsDialog
import com.leking.ads.dialog.NativeAdsDialog
import com.leking.ads.event.AdType
import com.leking.ads.event.FirebaseUtil
import java.security.MessageDigest
import java.util.Calendar

class Admob private constructor() {

    companion object {
        private const val TAG = "Admob"

        const val BANNER_INLINE_SMALL_STYLE = "BANNER_INLINE_SMALL_STYLE"
        const val BANNER_INLINE_LARGE_STYLE = "BANNER_INLINE_LARGE_STYLE"
        private const val MAX_SMALL_INLINE_BANNER_HEIGHT = 50

        @JvmField var isShowAdsDeviceTest = false
        @JvmField var isDeviceTest = false
        @JvmField var isShowAllAds = true
        @JvmField var timeLimitAds = 0L

        @Volatile private var INSTANCE: Admob? = null

        @JvmStatic
        fun getInstance(): Admob = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Admob().also { INSTANCE = it }
        }
    }

    private var context: Context? = null
    private var dialog: LoadingAdsDialog? = null
    private var currentClicked = 0
    private var numShowAds = 3
    private var maxClickAds = 10
    private var handlerTimeout: Handler? = null
    private var rdTimeout: Runnable? = null
    private var handlerShowNativeAll = Handler(Looper.getMainLooper())
    private var handlerDismissDialogLoading = Handler(Looper.getMainLooper())
    private var handler = Handler(Looper.getMainLooper())
    private var handlerNT = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var runnableNT: Runnable? = null

    private var isShowLoadingSplash = false
    private var openActivityAfterShowInterAds = true
    private var isTimeDelay = false
    private var isTimeout = false
    private var isShowNative = true
    private var checkLoadBanner = false
    private var checkLoadBannerCollap = false
    private var disableAdResumeWhenClickAds = false
    private var timeLimitShowAds = 0L
    private var timeLimitShowAdsPresent = 0L

    private var rewardedId: String? = null
    @JvmField var rewardedAd: RewardedAd? = null
    private var countDownTimerNative: CountDownTimer? = null
    private var mInterstitialSplash: InterstitialAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var adsTestNative = "ca-app-pub-3940256099942544/2247696110"
    private var checkDeviceTest = "Test ad"
    private var checkDeviceTest2 = "Test Ads"
    private var timeCountdownNativeCollab = 15000L
    private var countdownGlobal: CountdownManager? = null

    fun setContext(context: Context) {
        this.context = context.applicationContext
    }

    fun setValueNativeAll(value: NativeAd?) {
        AdsUtils.nativeDialogAd = value
    }

    @JvmOverloads
    fun initAdmob(context: Context, testDeviceList: List<String>? = null) {
        Helper.setupAdmobData(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            val packageName = context.packageName
            if (packageName != processName) WebView.setDataDirectorySuffix(processName)
        }
        if (!testDeviceList.isNullOrEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(testDeviceList).build()
            )
        }
        MobileAds.initialize(context) {}
        this.context = context.applicationContext
    }

    fun setDisableAdResumeWhenClickAds(disableAdResumeWhenClickAds: Boolean) {
        this.disableAdResumeWhenClickAds = disableAdResumeWhenClickAds
    }

    fun setTimeLimitShowAds(timeLimitAds: Long) {
        this.timeLimitShowAds = timeLimitAds
    }

    fun setOpenShowAllAds(isShowAllAds: Boolean) {
        Admob.isShowAllAds = isShowAllAds
    }

    fun setShowAdsDeviceTest(isShowAds: Boolean) {
        isShowAdsDeviceTest = isShowAds
    }

    fun setOpenActivityAfterShowInterAds(openActivityAfterShowInterAds: Boolean) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds
    }

    /* ---------------- Banner ---------------- */

    fun loadBanner(activity: Activity, id: String) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) {
            hideBannerContainers(adContainer, shimmer)
            return
        }
        loadBanner(activity, id, adContainer, shimmer, object : BannerCallBack() {
            override fun onAdClicked() {
                loadBanner(activity, id)
            }
        }, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBanner(activity: Activity, id: String, callback: BannerCallBack?) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, callback, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBanner(activity: Activity, ids: List<String>?) {
        loadBanner(activity, ids, null)
    }

    fun loadBanner(activity: Activity, ids: List<String>?, callback: BannerCallBack?) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds() || ids.isNullOrEmpty()) {
            hideBannerContainers(adContainer, shimmer)
            return
        }
        checkLoadBanner = false
        loadBanner(activity, ids.toMutableList(), adContainer, shimmer, callback, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBanner(activity: Activity, id: String, useInlineAdaptive: Boolean) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBanner(activity: Activity, id: String, callback: BannerCallBack?, useInlineAdaptive: Boolean) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, callback, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadInlineBanner(activity: Activity, id: String, inlineStyle: String) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBanner(activity, id, adContainer, shimmer, null, true, inlineStyle)
    }

    fun loadInlineBanner(activity: Activity, id: String, inlineStyle: String, callback: BannerCallBack?) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, callback, true, inlineStyle)
    }

    fun loadCollapsibleBanner(activity: Activity, id: String) {
        loadCollapsibleBanner(activity, id, BannerGravity.bottom)
    }

    fun loadCollapsibleBanner(activity: Activity, id: String, gravity: String) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadCollapsibleBanner(activity, id, gravity, adContainer, shimmer)
    }

    fun loadCollapsibleBanner(activity: Activity, id: String, timeDelay: Int) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        destroyCollapse(adContainer)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadCollapsibleBanner(activity, id, BannerGravity.bottom, adContainer, shimmer, timeDelay)
    }

    fun loadCollapsibleBannerFloor(activity: Activity, ids: List<String>?, gravity: String) {
        loadCollapsibleBannerFloor(activity, ids, gravity, null)
    }

    fun loadCollapsibleBannerFloor(activity: Activity, ids: List<String>?, gravity: String, callback: BannerCallBack?) {
        val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds() || ids.isNullOrEmpty()) {
            hideBannerContainers(adContainer, shimmer)
            return
        }
        checkLoadBannerCollap = false
        loadCollapsibleBannerFloor(activity, ids.toMutableList(), gravity, adContainer, shimmer, callback)
    }

    fun loadBannerFragment(activity: Activity, ids: List<String>?, rootView: View) {
        loadBannerFragment(activity, ids, rootView, null)
    }

    fun loadBannerFragment(activity: Activity, ids: List<String>?, rootView: View, callback: BannerCallBack?) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds() || ids.isNullOrEmpty()) {
            hideBannerContainers(adContainer, shimmer)
            return
        }
        checkLoadBanner = false
        loadBanner(activity, ids.toMutableList(), adContainer, shimmer, callback, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBannerFragment(activity: Activity, id: String, rootView: View) {
        loadBannerFragment(activity, id, rootView, null)
    }

    fun loadBannerFragment(activity: Activity, id: String, rootView: View, callback: BannerCallBack?) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, callback, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBannerFragment(activity: Activity, id: String, rootView: View, useInlineAdaptive: Boolean) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBannerFragment(activity: Activity, id: String, rootView: View, callback: BannerCallBack?, useInlineAdaptive: Boolean) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, callback, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadInlineBannerFragment(activity: Activity, id: String, rootView: View, inlineStyle: String) {
        loadInlineBannerFragment(activity, id, rootView, inlineStyle, null)
    }

    fun loadInlineBannerFragment(activity: Activity, id: String, rootView: View, inlineStyle: String, callback: BannerCallBack?) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds()) hideBannerContainers(adContainer, shimmer)
        else loadBanner(activity, id, adContainer, shimmer, callback, true, inlineStyle)
    }

    fun loadCollapsibleBannerFragment(activity: Activity, id: String, rootView: View, gravity: String) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        loadCollapsibleBanner(activity, id, gravity, adContainer, shimmer)
    }

    fun loadCollapsibleBannerFragment(activity: Activity, ids: List<String>?, rootView: View, gravity: String) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val shimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        prepareBannerContainers(adContainer, shimmer)
        if (!canShowAds() || ids.isNullOrEmpty()) hideBannerContainers(adContainer, shimmer)
        else loadCollapsibleBannerFloor(activity, ids.toMutableList(), gravity, adContainer, shimmer, null)
    }

    private fun prepareBannerContainers(adContainer: FrameLayout?, shimmer: ShimmerFrameLayout?) {
        adContainer?.visibility = View.GONE
        shimmer?.visibility = View.VISIBLE
        shimmer?.startShimmer()
    }

    private fun hideBannerContainers(adContainer: FrameLayout?, shimmer: ShimmerFrameLayout?) {
        shimmer?.stopShimmer()
        adContainer?.visibility = View.GONE
        shimmer?.visibility = View.GONE
    }

    private fun loadBanner(
        activity: Activity,
        id: String,
        adContainer: FrameLayout?,
        shimmer: ShimmerFrameLayout?,
        callback: BannerCallBack?,
        useInlineAdaptive: Boolean,
        inlineStyle: String
    ) {
        if (adContainer == null || shimmer == null) return
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        runCatching {
            adContainer.removeAllViews()
            val adView = AdView(activity).apply { adUnitId = id }
            val adSize = getAdSize(activity, useInlineAdaptive, inlineStyle)
            val adHeight = if (useInlineAdaptive && inlineStyle.equals(BANNER_INLINE_SMALL_STYLE, true)) MAX_SMALL_INLINE_BANNER_HEIGHT else adSize.height
            shimmer.layoutParams?.height = (adHeight * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
            adView.setAdSize(adSize)
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            adContainer.addView(adView)
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    hideBannerContainers(adContainer, shimmer)
                    callback?.onAdFailedToLoad(error)
                }
                override fun onAdLoaded() {
                    shimmer.stopShimmer()
                    shimmer.visibility = View.GONE
                    adContainer.visibility = View.VISIBLE
                    callback?.onAdLoadSuccess()
                    adView.setOnPaidEventListener { value ->
                        context?.let { FirebaseUtil.logPaidAdImpression(it, value, adView.adUnitId, AdType.BANNER) }
                        callback?.onEarnRevenue(value.valueMicros.toDouble())
                    }
                }
                override fun onAdClicked() {
                    callback?.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction()
                    context?.let { FirebaseUtil.logClickAdsEvent(it, id) }
                }
                override fun onAdImpression() {
                    callback?.onAdImpression()
                }
            }
            adView.loadAd(getAdRequest())
        }.onFailure { hideBannerContainers(adContainer, shimmer) }
    }

    private fun loadBanner(
        activity: Activity,
        ids: MutableList<String>,
        adContainer: FrameLayout?,
        shimmer: ShimmerFrameLayout?,
        callback: BannerCallBack?,
        useInlineAdaptive: Boolean,
        inlineStyle: String
    ) {
        if (checkLoadBanner) return
        if (ids.isEmpty()) {
            hideBannerContainers(adContainer, shimmer)
            return
        }
        val id = ids.first()
        loadBanner(activity, id, adContainer, shimmer, object : BannerCallBack() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError?) {
                callback?.onAdFailedToLoad(loadAdError)
                if (ids.isNotEmpty()) ids.removeAt(0)
                loadBanner(activity, ids, adContainer, shimmer, callback, useInlineAdaptive, inlineStyle)
            }
            override fun onAdLoadSuccess() {
                checkLoadBanner = true
                callback?.onAdLoadSuccess()
            }
            override fun onAdClicked() = callback?.onAdClicked() ?: Unit
            override fun onAdImpression() = callback?.onAdImpression() ?: Unit
            override fun onEarnRevenue(revenue: Double) = callback?.onEarnRevenue(revenue) ?: Unit
        }, useInlineAdaptive, inlineStyle)
    }

    private fun loadCollapsibleBanner(
        activity: Activity,
        id: String,
        gravity: String,
        adContainer: FrameLayout?,
        shimmer: ShimmerFrameLayout?,
        timeDelay: Int = 0
    ) {
        if (adContainer == null || shimmer == null) return
        if (!isNetworkConnected()) {
            shimmer.visibility = View.GONE
            return
        }
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        runCatching {
            adContainer.removeAllViews()
            val adView = AdView(activity).apply { adUnitId = id }
            val adSize = getAdSize(activity, false, "")
            shimmer.layoutParams?.height = (adSize.height * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
            adView.setAdSize(adSize)
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            adContainer.addView(adView)
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) = hideBannerContainers(adContainer, shimmer)
                override fun onAdLoaded() {
                    shimmer.stopShimmer()
                    shimmer.visibility = View.GONE
                    adContainer.visibility = View.VISIBLE
                    adView.setOnPaidEventListener { value -> context?.let { FirebaseUtil.logPaidAdImpression(it, value, adView.adUnitId, AdType.BANNER) } }
                }
                override fun onAdClicked() {
                    if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction()
                    context?.let { FirebaseUtil.logClickAdsEvent(it, id) }
                }
                override fun onAdImpression() {
                    if (timeDelay > 0) {
                        runnable?.let { handler.removeCallbacks(it) }
                        runnable = Runnable { loadCollapsibleBanner(activity, id, timeDelay) }
                        handler.postDelayed(runnable!!, timeDelay.toLong())
                    }
                }
            }
            adView.loadAd(getAdRequestForCollapsibleBanner(gravity))
        }.onFailure { hideBannerContainers(adContainer, shimmer) }
    }

    private fun loadCollapsibleBannerFloor(
        activity: Activity,
        ids: MutableList<String>,
        gravity: String,
        adContainer: FrameLayout?,
        shimmer: ShimmerFrameLayout?,
        callback: BannerCallBack?
    ) {
        if (checkLoadBannerCollap) return
        if (adContainer == null || shimmer == null || ids.isEmpty()) {
            hideBannerContainers(adContainer, shimmer)
            return
        }
        val id = ids.first()
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        runCatching {
            adContainer.removeAllViews()
            val adView = AdView(activity).apply { adUnitId = id }
            val adSize = getAdSize(activity, false, "")
            shimmer.layoutParams?.height = (adSize.height * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
            adView.setAdSize(adSize)
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            adContainer.addView(adView)
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    callback?.onAdFailedToLoad(error)
                    if (ids.isNotEmpty()) ids.removeAt(0)
                    loadCollapsibleBannerFloor(activity, ids, gravity, adContainer, shimmer, callback)
                }
                override fun onAdLoaded() {
                    checkLoadBannerCollap = true
                    shimmer.stopShimmer()
                    shimmer.visibility = View.GONE
                    adContainer.visibility = View.VISIBLE
                    callback?.onAdLoadSuccess()
                    adView.setOnPaidEventListener { value -> context?.let { FirebaseUtil.logPaidAdImpression(it, value, adView.adUnitId, AdType.BANNER) } }
                }
                override fun onAdClicked() {
                    callback?.onAdClicked()
                    if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction()
                    context?.let { FirebaseUtil.logClickAdsEvent(it, id) }
                }
            }
            adView.loadAd(getAdRequestForCollapsibleBanner(gravity))
        }.onFailure { hideBannerContainers(adContainer, shimmer) }
    }

    private fun destroyCollapse(viewGroup: ViewGroup?) {
        if (viewGroup == null) return
        for (i in 0 until viewGroup.childCount) {
            when (val child = viewGroup.getChildAt(i)) {
                is AdView -> child.destroy()
                is ViewGroup -> destroyCollapse(child)
            }
        }
    }

    private fun getAdSize(activity: Activity, useInlineAdaptive: Boolean, inlineStyle: String): AdSize {
        val outMetrics = DisplayMetrics()
        @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(outMetrics)
        val adWidth = (outMetrics.widthPixels / outMetrics.density).toInt()
        return if (useInlineAdaptive) {
            if (inlineStyle.equals(BANNER_INLINE_LARGE_STYLE, true)) {
                AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, adWidth)
            } else {
                AdSize.getInlineAdaptiveBannerAdSize(adWidth, MAX_SMALL_INLINE_BANNER_HEIGHT)
            }
        } else {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }
    }

    private fun getAdRequestForCollapsibleBanner(gravity: String): AdRequest {
        val extras = Bundle().apply { putString("collapsible", gravity) }
        return AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
    }

    /* ---------------- Splash Inter ---------------- */

    fun interstitialSplashLoaded(): Boolean = mInterstitialSplash != null
    fun getmInterstitialSplash(): InterstitialAd? = mInterstitialSplash
    fun getRewardedAd(): RewardedAd? = rewardedAd

    fun loadSplashInterAds(context: Context, id: String, timeOut: Long, timeDelay: Long, adListener: InterCallback?) {
        isTimeDelay = false
        isTimeout = false
        if (!isNetworkConnected()) {
            Handler(Looper.getMainLooper()).postDelayed({ adListener?.onAdClosed(); adListener?.onNextAction() }, 3000)
            return
        }
        val activity = context as? Activity ?: run {
            adListener?.onAdClosed(); adListener?.onNextAction(); return
        }
        AdsConsentManager(activity).requestUMP { canRequest ->
            if (canRequest) initAdmob(context, null)
            Handler(Looper.getMainLooper()).postDelayed({
                if (mInterstitialSplash != null) onShowSplash(activity, adListener) else isTimeDelay = true
            }, timeDelay)
            if (timeOut > 0) {
                handlerTimeout = Handler(Looper.getMainLooper())
                rdTimeout = Runnable {
                    isTimeout = true
                    if (mInterstitialSplash != null) onShowSplash(activity, adListener)
                    else {
                        adListener?.onAdClosed()
                        adListener?.onNextAction()
                        isShowLoadingSplash = false
                    }
                }
                handlerTimeout?.postDelayed(rdTimeout!!, timeOut)
            }
            isShowLoadingSplash = true
            loadInterAds(context, id, object : InterCallback() {
                override fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {
                    if (isTimeout) return
                    mInterstitialSplash = interstitialAd
                    if (isTimeDelay && interstitialAd != null) onShowSplash(activity, adListener)
                }
                override fun onAdFailedToLoad(error: LoadAdError?) {
                    if (isTimeout) return
                    rdTimeout?.let { handlerTimeout?.removeCallbacks(it) }
                    adListener?.onAdFailedToLoad(error)
                    adListener?.onNextAction()
                }
                override fun onAdClicked() {
                    if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction()
                }
            })
        }
    }

    fun loadSplashInterAds2(context: Context, id: String, timeDelay: Long, adListener: InterCallback?) {
        if (!isNetworkConnected() || !isShowAllAds) {
            Handler(Looper.getMainLooper()).postDelayed({ adListener?.onAdClosed(); adListener?.onNextAction() }, 3000)
            return
        }
        val activity = context as? Activity ?: run { adListener?.onNextAction(); return }
        AdsConsentManager(activity).requestUMP { canRequest ->
            if (canRequest) initAdmob(context, null)
            mInterstitialSplash = null
            Handler(Looper.getMainLooper()).postDelayed({
                InterstitialAd.load(context, id, getAdRequest(), object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        mInterstitialSplash = ad
                        AppOpenManager.getInstance().disableAppResume()
                        onShowSplash(activity, adListener)
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        mInterstitialSplash = null
                        adListener?.onAdFailedToLoad(error)
                        adListener?.onNextAction()
                    }
                })
            }, timeDelay)
        }
    }

    fun onShowSplash(activity: Activity, interSplash: InterstitialAd?, adListener: InterCallback?) {
        mInterstitialSplash = interSplash
        onShowSplash(activity, adListener)
    }

    private fun onShowSplash(activity: Activity, adListener: InterCallback?) {
        isShowLoadingSplash = true
        val ad = mInterstitialSplash
        if (ad == null) {
            adListener?.onAdClosed()
            adListener?.onNextAction()
            return
        }
        ad.setOnPaidEventListener { value ->
            context?.let { FirebaseUtil.logPaidAdImpression(it, value, ad.adUnitId, AdType.INTERSTITIAL) }
            adListener?.onEarnRevenue(value.valueMicros.toDouble())
        }
        rdTimeout?.let { handlerTimeout?.removeCallbacks(it) }
        adListener?.onAdLoaded()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().disableAppResume()
                isShowLoadingSplash = false
                adListener?.onAdImpression()
            }
            override fun onAdDismissedFullScreenContent() {
                if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().enableAppResume()
                dismissDialog()
                mInterstitialSplash = null
                isShowLoadingSplash = false
                if (!openActivityAfterShowInterAds) {
                    adListener?.onAdClosed()
                    adListener?.onNextAction()
                } else {
                    adListener?.onAdClosedByUser()
                }
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                mInterstitialSplash = null
                isShowLoadingSplash = false
                dismissDialog()
                adListener?.onAdFailedToShow(error)
                if (!openActivityAfterShowInterAds) adListener?.onNextAction()
            }
            override fun onAdClicked() {
                if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction()
                context?.let { FirebaseUtil.logClickAdsEvent(it, ad.adUnitId) }
                adListener?.onAdClicked()
            }
        }
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            isShowLoadingSplash = false
            return
        }
        runCatching {
            dismissDialog()
            dialog = LoadingAdsDialog(activity).also { it.show() }
        }.onFailure {
            adListener?.onAdClosed()
            adListener?.onNextAction()
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (activity.isFinishing || activity.isDestroyed) {
                dismissDialog()
                adListener?.onAdClosed()
                adListener?.onNextAction()
                isShowLoadingSplash = false
            } else {
                if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().disableAppResume()
                if (openActivityAfterShowInterAds) {
                    adListener?.onAdClosed()
                    adListener?.onNextAction()
                    Handler(Looper.getMainLooper()).postDelayed({ dismissDialog() }, 1500)
                }
                ad.show(activity)
            }
        }, 500)
    }

    /* ---------------- Inter ---------------- */

    fun loadInterAll(context: Context, id: String) {
        if (!isShowAllAds || AdsUtils.interAllReady) return
        InterstitialAd.load(context, id, getAdRequest(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                AdsUtils.interstitialAd = ad
                AdsUtils.interAllReady = true
                ad.setOnPaidEventListener { value -> FirebaseUtil.logPaidAdImpression(context, value, ad.adUnitId, AdType.INTERSTITIAL) }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                AdsUtils.interAllReady = false
            }
        })
    }

    fun setIDNativeAll(id: String) {
        AdsUtils.idNativeAll = id
    }

    fun loadNativeAll(context: Context, id: String) {
        if (!isShowAllAds) return
        if (id.isNotBlank()) setIDNativeAll(id)
        loadNativeAd(context, id, object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd) { AdsUtils.nativeDialogAd = nativeAd }
            override fun onAdFailedToLoad() { AdsUtils.nativeDialogAd = null }
        })
    }

    fun loadInterAds(context: Context, id: String, adCallback: InterCallback?) {
        if (!isShowAllAds) {
            adCallback?.onNextAction()
            adCallback?.onAdFailedToLoad(null)
            return
        }
        adCallback?.onAdLoaded()
        isTimeout = false
        interstitialAd = null
        InterstitialAd.load(context, id, getAdRequest(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                adCallback?.onAdLoadSuccess(ad)
                ad.setOnPaidEventListener { value ->
                    FirebaseUtil.logPaidAdImpression(context, value, ad.adUnitId, AdType.INTERSTITIAL)
                    adCallback?.onEarnRevenue(value.valueMicros.toDouble())
                }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                adCallback?.onAdFailedToLoad(error)
                adCallback?.onNextAction()
            }
        })
    }

    fun loadInterAdsNotLimit(context: Context, id: String, adCallback: InterCallback?) = loadInterAds(context, id, adCallback)

    fun loadInterAds(context: Context, ids: List<String>?, adCallback: InterCallback?) {
        if (ids.isNullOrEmpty()) {
            adCallback?.onAdFailedToLoad(null)
            adCallback?.onNextAction()
            return
        }
        loadInterAdsFloorByList(context, ids.toMutableList(), adCallback)
    }

    private fun loadInterAdsFloorByList(context: Context, ids: MutableList<String>, adCallback: InterCallback?) {
        if (!isShowAllAds || ids.isEmpty()) {
            adCallback?.onAdFailedToLoad(null)
            adCallback?.onNextAction()
            return
        }
        if (!checkTimeShowInter(timeLimitShowAds)) return
        val id = ids.first()
        adCallback?.onAdLoaded()
        InterstitialAd.load(context, id, getAdRequest(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                adCallback?.onAdLoadSuccess(ad)
                ad.setOnPaidEventListener { value ->
                    FirebaseUtil.logPaidAdImpression(context, value, ad.adUnitId, AdType.INTERSTITIAL)
                    adCallback?.onEarnRevenue(value.valueMicros.toDouble())
                }
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                adCallback?.onAdFailedToLoad(error)
                if (ids.isNotEmpty()) ids.removeAt(0)
                loadInterAdsFloorByList(context, ids, adCallback)
            }
        })
    }

    fun showInterAds(context: Context, ad: InterstitialAd?, callback: InterCallback?) {
        showInterAds(context, ad, callback, false, false)
    }

    fun showInterAll(context: Context, callback: InterCallback?) {
        showInterAds(context, AdsUtils.interstitialAd, callback, true, true)
    }

    fun showInterAds(context: Context, ad: InterstitialAd?, callback: InterCallback?, shouldReload: Boolean, limitTime: Boolean) {
        currentClicked = numShowAds
        showInterAdByTimes(context, ad, callback, shouldReload, limitTime)
    }

    private fun showInterAdByTimes(context: Context, ad: InterstitialAd?, callback: InterCallback?, shouldReload: Boolean, limitTime: Boolean) {
        Helper.setupAdmobData(context)
        if (!isShowAllAds || isDeviceTest || ad == null || !checkTimeShowInterNotUpdate(timeLimitShowAds)) {
            callback?.onAdClosed()
            callback?.onNextAction()
            return
        }
        val nativeDialog = NativeAdsDialog(context)
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().enableAppResume()
                if (shouldReload && limitTime) {
                    AdsUtils.interAllReady = false
                    loadInterAll(context, ad.adUnitId)
                }
                dismissDialog()
                setTimeShowInterAll()
                dismissInterWithOnNextAction(callback)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                if (shouldReload && limitTime) {
                    AdsUtils.interAllReady = false
                    loadInterAll(context, ad.adUnitId)
                }
                dismissDialog()
                callback?.onAdFailedToShow(error)
                if (!openActivityAfterShowInterAds) {
                    callback?.onAdClosed()
                    callback?.onNextAction()
                }
            }
            override fun onAdShowedFullScreenContent() {
                callback?.onAdImpression()
                if (limitTime && timeLimitShowAds > 1000) setTimeShowInterAll()
            }
            override fun onAdClicked() {
                callback?.onAdClicked()
                if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction()
                FirebaseUtil.logClickAdsEvent(context, ad.adUnitId)
            }
        }
        if (Helper.getNumClickAdsPerDay(context, ad.adUnitId) >= maxClickAds) {
            callback?.onAdClosed()
            callback?.onNextAction()
            return
        }
        if (limitTime) showInterstitialAd(context, ad, callback, nativeDialog) else showInterstitialAdNotLimit(context, ad, callback, nativeDialog)
    }

    private fun showInterstitialAd(context: Context, ad: InterstitialAd, callback: InterCallback?, nativeDialog: NativeAdsDialog) {
        if (!checkTimeShowInter(timeLimitShowAds) || !isShowAllAds || !isNetworkConnected()) {
            callback?.onAdClosed(); callback?.onNextAction(); return
        }
        showInterstitialAdNotLimit(context, ad, callback, nativeDialog)
    }

    private fun showInterstitialAdNotLimit(context: Context, ad: InterstitialAd, callback: InterCallback?, nativeDialog: NativeAdsDialog) {
        if (!isShowAllAds || !isNetworkConnected()) {
            callback?.onAdClosed(); callback?.onNextAction(); return
        }
        val activity = context as? Activity ?: run { callback?.onAdClosed(); callback?.onNextAction(); return }
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            callback?.onAdClosed(); callback?.onNextAction(); return
        }
        runCatching {
            dismissDialog()
            dialog = LoadingAdsDialog(context).also { it.show() }
        }.onFailure {
            callback?.onAdClosed(); callback?.onNextAction(); return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().disableAppResume()
            checkNativeAll(ad, callback, nativeDialog)
            ad.show(activity)
        }, 800)
    }

    private fun checkNativeAll(ad: InterstitialAd, callback: InterCallback?, nativeDialog: NativeAdsDialog) {
        if (AdsUtils.idNativeAll.isBlank()) {
            if (openActivityAfterShowInterAds) showInterWithOnNextAction(callback)
            return
        }
        if (ad.responseInfo?.mediationAdapterClassName?.contains("admob", true) == true && AdsUtils.nativeDialogAd != null) {
            handlerShowNativeAll.postDelayed({
                dismissDialog()
                runCatching { nativeDialog.show() }.onFailure { if (openActivityAfterShowInterAds) showInterWithOnNextAction(callback) }
            }, 1500)
        } else if (openActivityAfterShowInterAds) {
            showInterWithOnNextAction(callback)
        }
    }

    private fun showInterWithOnNextAction(callback: InterCallback?) {
        callback?.onAdClosed()
        callback?.onNextAction()
        handlerDismissDialogLoading.postDelayed({ dismissDialog() }, 1500)
    }

    private fun dismissInterWithOnNextAction(callback: InterCallback?) {
        if (!openActivityAfterShowInterAds) {
            callback?.onAdClosed()
            callback?.onNextAction()
        } else {
            callback?.onAdClosedByUser()
        }
    }

    /* ---------------- Reward ---------------- */

    fun showRewardAds(activity: Activity, callback: RewardCallback?) {
        val ad = rewardedAd
        if (!isShowAllAds || !isNetworkConnected()) { callback?.onAdClosed(); return }
        if (ad == null) { rewardedId?.let { initRewardAds(activity, it) }; callback?.onAdFailedToShow(0); return }
        showRewardAds(activity, callback, ad)
    }

    fun showRewardAds(activity: Activity, callback: RewardCallback?, rewardedAd: RewardedAd?) {
        val ad = rewardedAd
        if (!isShowAllAds || !isNetworkConnected()) { callback?.onAdClosed(); return }
        if (ad == null) { callback?.onAdFailedToShow(0); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { callback?.onAdClosed(); if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().enableAppResume() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { callback?.onAdFailedToShow(error.code) }
            override fun onAdShowedFullScreenContent() { if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().disableAppResume(); this@Admob.rewardedAd = null; callback?.onAdImpression() }
            override fun onAdClicked() { if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction(); FirebaseUtil.logClickAdsEvent(activity, ad.adUnitId) }
        }
        ad.show(activity) { rewardItem: RewardItem -> callback?.onEarnedReward(rewardItem) }
    }

    fun initRewardAds(context: Context, id: String) {
        if (!isShowAllAds) return
        rewardedId = id
        RewardedAd.load(context, id, getAdRequest(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                ad.setOnPaidEventListener { value -> FirebaseUtil.logPaidAdImpression(context, value, ad.adUnitId, AdType.REWARDED) }
            }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
        })
    }

    fun loadAndShowRewardAds(context: Context, id: String, callback: RewardCallback?) {
        val activity = context as? Activity ?: run { callback?.onAdFailedToLoad(); return }
        if (!isShowAllAds || !isNetworkConnected()) { callback?.onAdFailedToLoad(); return }
        runCatching { dismissDialog(); dialog = LoadingAdsDialog(context).also { it.show() } }
        Handler(Looper.getMainLooper()).postDelayed({
            if (AppOpenManager.getInstance().isInitialized()) AppOpenManager.getInstance().disableAppResume()
            RewardedAd.load(context, id, getAdRequest(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    dismissDialog()
                    callback?.onAdLoaded(ad)
                    showRewardAds(activity, callback, ad)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    dismissDialog()
                    callback?.onAdFailedToLoad()
                }
            })
        }, 300)
    }

    /* ---------------- Native ---------------- */

    fun loadNativeAd(context: Context, id: String, callback: NativeCallback?) {
        if (!canShowAds() || !isShowNative) { callback?.onAdFailedToLoad(); return }
        val options = NativeAdOptions.Builder().setVideoOptions(VideoOptions.Builder().setStartMuted(true).build()).build()
        val loader = AdLoader.Builder(context, id)
            .forNativeAd { nativeAd ->
                if (!checkDeviceTest(nativeAd, id)) callback?.onNativeAdLoaded(nativeAd) else callback?.onAdFailedToLoad()
                nativeAd.setOnPaidEventListener { value ->
                    FirebaseUtil.logPaidAdImpression(context, value, id, AdType.NATIVE)
                    callback?.onEarnRevenue(value.valueMicros.toDouble())
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) { callback?.onAdFailedToLoad() }
                override fun onAdClicked() { callback?.onAdClicked(); if (disableAdResumeWhenClickAds) AppOpenManager.getInstance().disableAdResumeByClickAction(); FirebaseUtil.logClickAdsEvent(context, id) }
            })
            .withNativeAdOptions(options)
            .build()
        loader.loadAd(getAdRequest())
    }

    fun loadNativeAd(context: Context, id: String, frameLayout: FrameLayout, layoutNative: Int) {
        if (!canShowAds() || !isShowNative) { frameLayout.removeAllViews(); return }
        loadNativeAd(context, id, object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                val adView = LayoutInflater.from(context).inflate(layoutNative, null) as NativeAdView
                frameLayout.removeAllViews()
                frameLayout.addView(adView)
                pushAdsToViewCustom(nativeAd, adView)
            }
            override fun onAdFailedToLoad() { frameLayout.removeAllViews() }
        })
    }

    fun loadNativeAd(context: Context, ids: List<String>?, callback: NativeCallback?) {
        if (ids.isNullOrEmpty()) { callback?.onAdFailedToLoad(); return }
        val mutable = ids.toMutableList()
        loadNativeAd(context, mutable.first(), object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd) { callback?.onNativeAdLoaded(nativeAd) }
            override fun onAdClicked() { callback?.onAdClicked() }
            override fun onEarnRevenue(revenue: Double) { callback?.onEarnRevenue(revenue) }
            override fun onAdFailedToLoad() {
                mutable.removeAt(0)
                if (mutable.isEmpty()) callback?.onAdFailedToLoad() else loadNativeAd(context, mutable, callback)
            }
        })
    }

    fun loadNativeAd(context: Context, ids: List<String>?, frameLayout: FrameLayout, layoutNative: Int) {
        if (ids.isNullOrEmpty()) { frameLayout.removeAllViews(); return }
        val mutable = ids.toMutableList()
        loadNativeAd(context, mutable.first(), object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                val adView = LayoutInflater.from(context).inflate(layoutNative, null) as NativeAdView
                frameLayout.removeAllViews()
                frameLayout.addView(adView)
                pushAdsToViewCustom(nativeAd, adView)
            }
            override fun onAdFailedToLoad() {
                mutable.removeAt(0)
                if (mutable.isEmpty()) frameLayout.removeAllViews() else loadNativeAd(context, mutable, frameLayout, layoutNative)
            }
        })
    }

    fun pushAdsToViewCustom(nativeAd: NativeAd?, adView: NativeAdView?) {
        if (nativeAd == null || adView == null) return
        runCatching {
            adView.mediaView = adView.findViewById(R.id.ad_media)
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            adView.iconView = adView.findViewById(R.id.ad_app_icon)
            adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
            (adView.headlineView as? TextView)?.text = nativeAd.headline
            (adView.bodyView as? TextView)?.apply { text = nativeAd.body.orEmpty(); visibility = if (nativeAd.body == null) View.INVISIBLE else View.VISIBLE }
            (adView.callToActionView as? TextView)?.apply { text = nativeAd.callToAction.orEmpty(); visibility = if (nativeAd.callToAction == null) View.INVISIBLE else View.VISIBLE }
            (adView.iconView as? ImageView)?.apply {
                val icon = nativeAd.icon?.drawable
                if (icon != null) { setImageDrawable(icon); visibility = View.VISIBLE } else visibility = View.GONE
            }
//            (adView.findViewById<RatingBar?>(R.id.ad_star_rating))?.let { rb ->
//                adView.starRatingView = rb
//                nativeAd.starRating?.let { rb.rating = it.toFloat(); rb.visibility = View.VISIBLE } ?: run { rb.visibility = View.INVISIBLE }
//            }
            (adView.advertiserView as? TextView)?.apply { text = nativeAd.advertiser.orEmpty(); visibility = if (nativeAd.advertiser == null) View.INVISIBLE else View.VISIBLE }
            adView.mediaView?.let { media ->
                if (nativeAd.mediaContent != null) { media.mediaContent = nativeAd.mediaContent; media.visibility = View.VISIBLE } else media.visibility = View.GONE
            }
            adView.setNativeAd(nativeAd)
        }.onFailure { Log.e(TAG, "pushAdsToViewCustom: ${it.message}") }
    }

    fun removeAllViewsWithSlideDownAndFade(container: ViewGroup?, newView: View?) {
        if (container == null) return
        if (newView != null && newView.parent != null) (newView.parent as? ViewGroup)?.removeView(newView)
        if (newView != null) { newView.alpha = 0f; container.addView(newView) }
        val animatorSet = AnimatorSet()
        val animators = mutableListOf<Animator>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child != newView) {
                animators.add(ObjectAnimator.ofFloat(child, "translationY", child.height + 300f))
                animators.add(ObjectAnimator.ofFloat(child, "alpha", 0f))
            }
        }
        if (newView != null) animators.add(ObjectAnimator.ofFloat(newView, "alpha", 0f, 1f))
        animatorSet.playTogether(animators)
        animatorSet.duration = 100
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                for (i in container.childCount - 1 downTo 0) if (container.getChildAt(i) != newView) container.removeViewAt(i)
            }
        })
        animatorSet.start()
    }

    fun loadNativeCollap(context: Context, id: String, frameLayout: FrameLayout, layoutNative: Int, layoutBanner: Int) {
        loadNativeCollapInternal(context, id, frameLayout, layoutNative, layoutBanner, null, false)
    }

    fun loadNativeCollap(context: Context, id: String, frameLayout: FrameLayout, layoutNative: Int) {
        loadNativeCollapInternal(context, id, frameLayout, layoutNative, R.layout.ads_native_collap_banner, null, false)
    }

    fun loadNativeCollap(context: Context, id: String, frameLayout: FrameLayout) {
        loadNativeCollapInternal(context, id, frameLayout, R.layout.ads_native_collap_avg, R.layout.ads_native_collap_banner, null, false)
    }

    fun loadNativeCollap(context: Context, id: String, frameLayout: FrameLayout, callback: NativeCallback?) {
        loadNativeCollapInternal(context, id, frameLayout, R.layout.ads_native_collap_avg, R.layout.ads_native_collap_banner, callback, false)
    }

    fun loadNativeCollapNotBanner(context: Context, id: String, frameLayout: FrameLayout, callback: NativeCallback?) {
        loadNativeCollapInternal(context, id, frameLayout, R.layout.ads_native_collap_avg, 0, callback, true)
    }

    fun loadNativeCollapNotBanner(context: Context, id: String, frameLayout: FrameLayout) {
        loadNativeCollapInternal(context, id, frameLayout, R.layout.ads_native_collap_avg, 0, null, true)
    }

    private fun loadNativeCollapInternal(context: Context, id: String, frameLayout: FrameLayout, layoutNative: Int, layoutBanner: Int, callback: NativeCallback?, noBanner: Boolean) {
        if (!canShowAds() || !isShowNative) { frameLayout.removeAllViews(); callback?.onAdFailedToLoad(); return }
        loadNativeAd(context, id, object : NativeCallback() {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                val adView = LayoutInflater.from(context).inflate(layoutNative, null) as NativeAdView
                frameLayout.removeAllViews()
                frameLayout.addView(adView)
                pushAdsToViewCustom(nativeAd, adView)
                callback?.onNativeAdLoaded(nativeAd)
                adView.findViewById<ImageView?>(R.id.iv_down)?.setOnClickListener {
                    if (noBanner) {
                        frameLayout.removeAllViews()
                        callback?.onAdClicked()
                        startNewCountdown(timeCountdownNativeCollab, object : CountdownManager.CountdownListener {
                            override fun onTick(millisLeft: Long) {}
                            override fun onFinished() { loadNativeCollapNotBanner(context, id, frameLayout, callback) }
                        })
                    } else if (layoutBanner != 0) {
                        val banner = LayoutInflater.from(context).inflate(layoutBanner, null) as NativeAdView
                        removeAllViewsWithSlideDownAndFade(frameLayout, banner)
                        pushAdsToViewCustom(nativeAd, banner)
                    }
                }
            }
            override fun onAdFailedToLoad() { frameLayout.removeAllViews(); callback?.onAdFailedToLoad() }
            override fun onAdClicked() { callback?.onAdClicked() }
        })
    }

    fun setTimeCountdownNativeCollab(time: Long) { timeCountdownNativeCollab = time }

    private fun startNewCountdown(millis: Long, listener: CountdownManager.CountdownListener) {
        countdownGlobal?.cancel()
        countdownGlobal = CountdownManager(millis, listener).also { it.start() }
    }

    fun loadNativeBanner(context: Context, id: String, frameLayout: FrameLayout, timeDelay: Int, iconDown: Boolean) {
        loadNativeAd(context, id, frameLayout, R.layout.ads_native_collap_big)
        if (timeDelay > 0) {
            runnableNT?.let { handlerNT.removeCallbacks(it) }
            runnableNT = Runnable { loadNativeBanner(context, id, frameLayout, timeDelay, iconDown) }
            handlerNT.postDelayed(runnableNT!!, timeDelay.toLong())
        }
    }

    fun loadNativeAdHide(context: Context, id: String, frameLayout: FrameLayout, layoutNative: Int, timeDelay: Int, iconDown: Boolean) {
        loadNativeAd(context, id, frameLayout, layoutNative)
        if (timeDelay > 0) {
            countDownTimerNative?.cancel()
            countDownTimerNative = object : CountDownTimer(timeDelay.toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() { loadNativeAdHide(context, id, frameLayout, layoutNative, timeDelay, iconDown) }
            }.also { it.start() }
        }
    }

    /* ---------------- Utils ---------------- */

    val adRequest: AdRequest get() = getAdRequest()

    fun getAdRequest(): AdRequest = AdRequest.Builder().build()

    @SuppressLint("HardwareIds")
    fun getDeviceId(activity: Activity): String {
        val androidId = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID)
        return md5(androidId).uppercase()
    }

    private fun md5(value: String): String = runCatching {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(value.toByteArray())
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault("")

    private fun checkDeviceTest(nativeAd: NativeAd, id: String): Boolean {
        if (id == adsTestNative || isShowAdsDeviceTest) return false
        val headline = nativeAd.headline.orEmpty()
        val body = nativeAd.body.orEmpty()
        val isTest = checkContainsContent(headline) || checkContainsContent(body)
        if (isTest) {
            setOpenShowAllAds(false)
            AppOpenManager.getInstance().disableAppResume()
            isDeviceTest = true
        }
        return isTest
    }

    private fun checkContainsContent(content: String): Boolean {
        return content.lowercase().contains(checkDeviceTest.lowercase()) || content.lowercase().contains(checkDeviceTest2.lowercase())
    }

    private fun canShowAds(): Boolean = isShowAllAds && !isDeviceTest && isNetworkConnected()

    private fun isNetworkConnected(): Boolean {
        val ctx = context ?: return true
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        @Suppress("DEPRECATION") return cm.activeNetworkInfo?.isConnected == true
    }

    fun onCheckShowSplashWhenFail(activity: AppCompatActivity, callback: InterCallback?, timeDelay: Int) {
        if (!isNetworkConnected()) return
        Handler(activity.mainLooper).postDelayed({
            if (interstitialSplashLoaded() && !isShowLoadingSplash) onShowSplash(activity, callback)
        }, timeDelay.toLong())
    }

    fun onCheckShowSplashWhenFailClickButton(activity: AppCompatActivity, interstitialAd: InterstitialAd?, callback: InterCallback?, timeDelay: Int) {
        if (interstitialAd == null || !isNetworkConnected()) return
        Handler(activity.mainLooper).postDelayed({
            if (interstitialSplashLoaded() && !isShowLoadingSplash) onShowSplash(activity, interstitialAd, callback)
        }, timeDelay.toLong())
    }

    private fun setTimeShowInterAll() { timeLimitShowAdsPresent = System.currentTimeMillis() }

    fun checkTimeShowInter(timeDelay: Long): Boolean {
        if (timeDelay <= 0) return true
        val current = System.currentTimeMillis()
        return if (current - timeLimitShowAdsPresent > timeDelay) {
            setTimeShowInterAll()
            true
        } else false
    }

    fun checkTimeShowInterNotUpdate(timeDelay: Long): Boolean {
        if (timeDelay <= 0) return true
        return System.currentTimeMillis() - timeLimitShowAdsPresent > timeDelay
    }

    private fun dismissDialog() {
        runCatching { if (dialog?.isShowing == true) dialog?.dismiss() }
        dialog = null
    }
}
