package com.leking.ads.util

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.leking.ads.R

class Admob private constructor() {
    companion object {
        @JvmField var isShowAllAds: Boolean = true
        private val INSTANCE_ADMOB: Admob by lazy { Admob() }
        @JvmStatic fun getInstance(): Admob = INSTANCE_ADMOB
    }

    fun initAdmob(context: Context, testDeviceIds: List<String>? = null) {
        testDeviceIds?.takeIf { it.isNotEmpty() }?.let {
            MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(it).build())
        }
        MobileAds.initialize(context) {}
    }

    /** Bind NativeAd vào layout custom có các id chuẩn: ad_media, ad_headline, ad_body, ad_app_icon, ad_call_to_action, ad_advertiser. */
    fun pushAdsToViewCustom(nativeAd: NativeAd?, adView: NativeAdView?) {
        if (nativeAd == null || adView == null) return

        adView.findViewById<MediaView?>(R.id.ad_media)?.let {
            adView.mediaView = it
            it.mediaContent = nativeAd.mediaContent
        }
        adView.findViewById<TextView?>(R.id.ad_headline)?.let {
            adView.headlineView = it
            it.text = nativeAd.headline
        }
        adView.findViewById<TextView?>(R.id.ad_body)?.let {
            adView.bodyView = it
            it.text = nativeAd.body ?: ""
            it.visibility = if (nativeAd.body == null) View.GONE else View.VISIBLE
        }
        adView.findViewById<TextView?>(R.id.ad_advertiser)?.let {
            adView.advertiserView = it
            it.text = nativeAd.advertiser ?: ""
            it.visibility = if (nativeAd.advertiser == null) View.GONE else View.VISIBLE
        }
        adView.findViewById<ImageView?>(R.id.ad_app_icon)?.let {
            adView.iconView = it
            val icon = nativeAd.icon
            if (icon == null) it.visibility = View.GONE else {
                it.setImageDrawable(icon.drawable)
                it.visibility = View.VISIBLE
            }
        }
        val cta = adView.findViewById<View?>(R.id.ad_call_to_action)
        cta?.let {
            adView.callToActionView = it
            val text = nativeAd.callToAction ?: "Install"
            when (it) {
                is TextView -> it.text = text
                is Button -> it.text = text
                is AppCompatButton -> it.text = text
            }
            it.visibility = View.VISIBLE
        }
        adView.setNativeAd(nativeAd)
    }
}
