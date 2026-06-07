package com.lvt.ads.callback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
open class RewardCallback {
    open fun onEarnedReward(rewardItem: RewardItem) {}
    open fun onAdClosed() {}
    open fun onAdFailedToShow(codeError: Int) {}
    open fun onAdImpression() {}
    open fun onAdLoaded(rewardedAd: RewardedAd) {}
    open fun onAdFailedToLoad() {}
}
