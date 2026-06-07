package com.leking.ads.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.FrameLayout
import com.google.android.gms.ads.nativead.NativeAdView
import com.leking.ads.R

class NativeAdsDialog(context: Context) : Dialog(context, R.style.AdsDialogTheme) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_ads_native)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        // Tự inflate NativeAdView riêng của app rồi add vào native_ad_container nếu cần.
        findViewById<FrameLayout>(R.id.native_ad_container)?.removeAllViews()
    }
}
