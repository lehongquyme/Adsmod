package com.lvt.ads.callback
import com.google.android.gms.ads.LoadAdError
open class BannerCallBack {
    open fun onEarnRevenue(revenue: Double) {}
    open fun onAdFailedToLoad(loadAdError: LoadAdError?) {}
    open fun onAdLoadSuccess() {}
    open fun onAdClicked() {}
    open fun onAdImpression() {}
}
