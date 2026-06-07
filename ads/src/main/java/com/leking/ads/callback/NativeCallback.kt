package com.leking.ads.callback
import com.google.android.gms.ads.nativead.NativeAd
open class NativeCallback {
    open fun onNativeAdLoaded(nativeAd: NativeAd) {}
    open fun onAdFailedToLoad() {}
    open fun onEarnRevenue(revenue: Double) {}
    open fun onAdClicked() {}
}
