package com.leking.ads.util

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.leking.ads.callback.RewardCallback
import com.leking.ads.event.AdType
import com.leking.ads.event.FirebaseUtil

/**
 * Manager RewardedAd Kotlin.
 *
 * Cách dùng nhanh:
 * RewardAdManager.getInstance().loadRewardAd(context, idReward, callback)
 * RewardAdManager.getInstance().showRewardAd(activity, callback)
 *
 * Hoặc load + show luôn:
 * RewardAdManager.getInstance().loadAndShowRewardAd(activity, idReward, callback)
 */
class RewardAdManager private constructor() {

    companion object {
        private const val TAG = "RewardAdManager"
        private val INSTANCE: RewardAdManager by lazy { RewardAdManager() }

        @JvmStatic
        fun getInstance(): RewardAdManager = INSTANCE
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoadingReward = false
    private var currentRewardId: String? = null

    fun getRewardedAd(): RewardedAd? = rewardedAd

    fun isRewardReady(): Boolean = rewardedAd != null

    fun clearRewardAd() {
        rewardedAd = null
        isLoadingReward = false
    }

    @JvmOverloads
    fun loadRewardAd(
        context: Context,
        adUnitId: String,
        callback: RewardCallback? = null
    ) {
        if (!Admob.isShowAllAds) {
            callback?.onAdFailedToLoad()
            return
        }

        if (isLoadingReward) return

        if (rewardedAd != null && currentRewardId == adUnitId) {
            callback?.onAdLoaded(rewardedAd!!)
            return
        }

        isLoadingReward = true
        currentRewardId = adUnitId

        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Reward ad loaded")
                    isLoadingReward = false
                    rewardedAd = ad

                    ad.setOnPaidEventListener { adValue ->
                        FirebaseUtil.logPaidAdImpression(
                            context.applicationContext,
                            adValue,
                            ad.adUnitId,
                            AdType.REWARDED
                        )
                    }

                    callback?.onAdLoaded(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Reward ad failed to load: ${error.message}")
                    isLoadingReward = false
                    rewardedAd = null
                    callback?.onAdFailedToLoad()
                }
            }
        )
    }

    @JvmOverloads
    fun showRewardAd(
        activity: Activity,
        callback: RewardCallback? = null
    ) {
        val ad = rewardedAd
        if (ad == null) {
            callback?.onAdFailedToShow(-1)
            callback?.onAdClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Reward ad showed")
                rewardedAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Reward ad closed")
                rewardedAd = null
                callback?.onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Reward ad failed to show: ${adError.message}")
                rewardedAd = null
                callback?.onAdFailedToShow(adError.code)
                callback?.onAdClosed()
            }

            override fun onAdImpression() {
                callback?.onAdImpression()
            }
        }

        ad.show(activity) { rewardItem ->
            callback?.onEarnedReward(rewardItem)
        }
    }

    @JvmOverloads
    fun loadAndShowRewardAd(
        activity: Activity,
        adUnitId: String,
        callback: RewardCallback? = null,
        timeoutMillis: Long = 12000L
    ) {
        var finished = false
        val handler = Handler(Looper.getMainLooper())

        val timeoutRunnable = Runnable {
            if (!finished) {
                finished = true
                isLoadingReward = false
                callback?.onAdFailedToLoad()
                callback?.onAdClosed()
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMillis)

        loadRewardAd(activity, adUnitId, object : RewardCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                if (finished) return
                finished = true
                handler.removeCallbacks(timeoutRunnable)
                callback?.onAdLoaded(rewardedAd)
                showRewardAd(activity, callback)
            }

            override fun onAdFailedToLoad() {
                if (finished) return
                finished = true
                handler.removeCallbacks(timeoutRunnable)
                callback?.onAdFailedToLoad()
                callback?.onAdClosed()
            }
        })
    }
}
