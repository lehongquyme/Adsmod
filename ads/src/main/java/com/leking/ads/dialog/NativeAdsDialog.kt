package com.leking.ads.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.leking.ads.R

class NativeAdsDialog(context: Context) : Dialog(context, R.style.AdsDialogTheme) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_ads_native)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        val container = findViewById<FrameLayout>(R.id.native_ad_container)
        container.removeAllViews()
        val nativeView = LayoutInflater.from(context)
            .inflate(R.layout.ads_native_dialog, container, false)

        container.addView(nativeView)
    }
}