package com.bikeracing.game

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

enum class RewardType {
    DOUBLE_COINS,
    INSTANT_REPAIR,
    SPEED_BOOST,
    COIN_MAGNET,
    SHIELD,
    TIME_SLOW
}

interface AdRewardCallback {
    fun onRewardEarned(type: RewardType, amount: Int)
    fun onAdFailed(type: RewardType, error: String)
    fun onAdClosed(type: RewardType, wasRewarded: Boolean)
}

class AdManager(private val activity: Activity) {
    companion object {
        private const val TAG = "AdManager"
        private const val REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    }
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var bannerAd: AdView? = null
    private var isLoadingRewarded = false
    private var isLoadingInterstitial = false
    private var adRewardCallback: AdRewardCallback? = null
    private var pendingRewardType: RewardType? = null
    private var racesSinceLastInterstitial = 0
    private val interstitialFrequency = 3
    var adsRemoved = false
    fun initialize() {
        activity.runOnUiThread {
            MobileAds.initialize(activity) { initStatus ->
                Log.d(TAG, "AdMob initialized: ${initStatus.adapterStatusMap}")
                loadRewardedAd()
                loadInterstitialAd()
            }
        }
    }
    private fun loadRewardedAd() {
        if (isLoadingRewarded || rewardedAd != null || adsRemoved) return
        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_AD_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    setupRewardedAdCallbacks()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            })
    }
    private fun setupRewardedAdCallbacks() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                val wasRewarded = pendingRewardType == null
                val type = pendingRewardType
                if (type != null) {
                    adRewardCallback?.onAdClosed(type, false)
                }
                pendingRewardType = null
                rewardedAd = null
                loadRewardedAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                pendingRewardType?.let {
                    adRewardCallback?.onAdFailed(it, error.message)
                }
                pendingRewardType = null
                rewardedAd = null
                loadRewardedAd()
            }
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed")
            }
        }
    }
    private fun loadInterstitialAd() {
        if (isLoadingInterstitial || interstitialAd != null || adsRemoved) return
        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, INTERSTITIAL_AD_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    setupInterstitialCallbacks()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            })
    }
    private fun setupInterstitialCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                interstitialAd = null
                loadInterstitialAd()
            }
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed")
            }
        }
    }
    fun showRewardedAd(rewardType: RewardType, callback: AdRewardCallback) {
        if (adsRemoved) {
            callback.onRewardEarned(rewardType, getRewardAmount(rewardType))
            return
        }
        adRewardCallback = callback
        pendingRewardType = rewardType
        activity.runOnUiThread {
            if (rewardedAd != null) {
                rewardedAd?.show(activity) { rewardItem ->
                    val amount = getRewardAmount(rewardType)
                    Log.d(TAG, "User earned reward: $rewardType, amount: $amount")
                    callback.onRewardEarned(rewardType, amount)
                    callback.onAdClosed(rewardType, true)
                    pendingRewardType = null
                }
            } else {
                callback.onAdFailed(rewardType, "Ad not ready")
                pendingRewardType = null
                loadRewardedAd()
            }
        }
    }
    private fun getRewardAmount(type: RewardType): Int {
        return when (type) {
            RewardType.DOUBLE_COINS -> 100
            RewardType.INSTANT_REPAIR -> 1
            RewardType.SPEED_BOOST -> 30
            RewardType.COIN_MAGNET -> 30
            RewardType.SHIELD -> 20
            RewardType.TIME_SLOW -> 15
        }
    }
    fun showInterstitialAd() {
        if (adsRemoved) return
        racesSinceLastInterstitial++
        if (racesSinceLastInterstitial < interstitialFrequency) {
            return
        }
        activity.runOnUiThread {
            if (interstitialAd != null) {
                interstitialAd?.show(activity)
                racesSinceLastInterstitial = 0
            } else {
                Log.d(TAG, "Interstitial ad not ready")
                loadInterstitialAd()
            }
        }
    }
    fun createBannerAd(): AdView {
        bannerAd = AdView(activity).apply {
            adUnitId = BANNER_AD_ID
            setAdSize(AdSize.BANNER)
            loadAd(AdRequest.Builder().build())
        }
        return bannerAd!!
    }
    fun showBanner() {
        if (adsRemoved) return
        activity.runOnUiThread {
            bannerAd?.visibility = android.view.View.VISIBLE
        }
    }
    fun hideBanner() {
        activity.runOnUiThread {
            bannerAd?.visibility = android.view.View.GONE
        }
    }
    fun isRewardedAdReady(): Boolean = rewardedAd != null || adsRemoved
    fun isInterstitialAdReady(): Boolean = interstitialAd != null
    fun removeAds() {
        adsRemoved = true
        rewardedAd = null
        interstitialAd = null
        hideBanner()
    }
    fun pause() {
        bannerAd?.pause()
    }
    fun resume() {
        bannerAd?.resume()
    }
    fun destroy() {
        bannerAd?.destroy()
        rewardedAd = null
        interstitialAd = null
    }
}
