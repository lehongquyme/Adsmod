package com.lvt.ads.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.lvt.ads.R

class LoadingAdsDialog(context: Context) : Dialog(context, R.style.AdsDialogTheme) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading_ads)
    }
}
