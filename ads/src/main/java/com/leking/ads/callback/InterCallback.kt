package com.lvt.ads.callback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
open class InterCallback {
    open fun onAdClosed() {}
    open fun onAdFailedToLoad(error: LoadAdError?) {}
    open fun onAdFailedToShow(error: AdError?) {}
    open fun onAdLeftApplication() {}
    open fun onAdLoaded() {}
    open fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {}
    open fun onAdClicked() {}
    open fun onAdImpression() {}
    open fun onAdClosedByUser() {}
    open fun onNextAction() {}
    open fun onEarnRevenue(revenue: Double) {}
}
