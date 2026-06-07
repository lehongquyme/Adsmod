package com.lvt.ads.util
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
object AdsUtils {
    @JvmField var interstitialAd: InterstitialAd? = null
    @JvmField var interAllReady = false
    @JvmField var nativeDialogAd: NativeAd? = null
    @JvmField var idNativeAll = ""
    @JvmField var currentNativeCollab = ""
}
