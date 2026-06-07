# Kotlin Admob Kit

Android Library Kotlin tương tự source Java `com.lvt.ads`.

## Import local module

Copy thư mục `ads` vào project app, sau đó thêm vào `settings.gradle`:

```gradle
include ':ads'
```

Trong `app/build.gradle`:

```gradle
dependencies {
    implementation project(':ads')
}
```

## Hoặc đẩy lên Git rồi dùng JitPack

1. Push repo này lên GitHub.
2. Tạo release/tag, ví dụ `v1.0.0`.
3. Trong project app thêm JitPack:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Thêm dependency:

```gradle
implementation 'com.github.USERNAME:REPO:v1.0.0'
```

## Khởi tạo nhanh

```kotlin
class MyApp : AdsApplication() {
    override fun enableAdsResume() = true
    override fun getListTestDeviceId(): List<String>? = null
    override fun getResumeAdId() = "ca-app-pub-3940256099942544/9257395921"
    override fun buildDebug() = BuildConfig.DEBUG
}
```

Trong `AndroidManifest.xml` app:

```xml
<application
    android:name=".MyApp"
    ...>

    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-3940256099942544~3347511713" />
</application>
```

## App open splash

```kotlin
AppOpenManager.getInstance().loadOpenAppAdSplash(
    context = this,
    idResumeSplash = "YOUR_APP_OPEN_AD_ID",
    timeDelay = 3000,
    timeOut = 8000,
    isShowAdIfReady = true,
    callback = object : AdCallback() {
        override fun onNextAction() {
            // mở màn tiếp theo
        }
    }
)
```

## Ghi chú

- Package giữ là `com.lvt.ads` để bạn dễ thay thế code cũ.
- Facebook/Appsflyer được để dạng optional/stub để project không bắt buộc cấu hình SDK ngoài.
- Nếu muốn đổi package để tránh trùng thư viện cũ, replace `com.lvt.ads` thành package riêng của bạn.

## Resource native ads đã kèm sẵn

Bản này đã thêm các resource bạn gửi: layout native average/big/small/collapsible, shimmer, dialog native, loading dialog và banner. Các layout dùng id chuẩn `ad_headline`, `ad_body`, `ad_media`, `ad_app_icon`, `ad_call_to_action`, `ad_advertiser` để `Admob.pushAdsToViewCustom()` bind `NativeAd` tự động.

## Rewarded Ad

```kotlin
RewardAdManager.getInstance().loadAndShowRewardAd(
    activity = this,
    adUnitId = getString(R.string.ads_test_reward),
    callback = object : RewardCallback() {
        override fun onEarnedReward(rewardItem: RewardItem) {
            // User earned reward here
        }

        override fun onAdClosed() {
            // Continue app flow
        }

        override fun onAdFailedToLoad() {
            // Continue app flow if ad cannot load
        }
    }
)
```
