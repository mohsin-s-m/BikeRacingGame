package com.bikeracing.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

class RewardSystem(private val adManager: AdManager) {
    private val prefs: Preferences = Gdx.app.getPreferences("BikeRacingPrefs")
    var totalCoins: Int
        get() = prefs.getInteger("totalCoins", 0)
        set(value) {
            prefs.putInteger("totalCoins", value)
            prefs.flush()
        }
    var currentRaceCoins = 0
    var pendingDoubleCoins = false
    var activePowerUps = mutableSetOf<RewardType>()
    private val powerUpTimers = mutableMapOf<RewardType, Float>()
    fun onRaceComplete(coinsEarned: Int) {
        currentRaceCoins = coinsEarned
    }
    fun requestDoubleCoinsReward(callback: (Boolean) -> Unit) {
        if (!adManager.isRewardedAdReady()) {
            callback(false)
            return
        }
        adManager.showRewardedAd(RewardType.DOUBLE_COINS, object : AdRewardCallback {
            override fun onRewardEarned(type: RewardType, amount: Int) {
                val doubledCoins = currentRaceCoins * 2
                totalCoins += doubledCoins
                pendingDoubleCoins = true
                Gdx.app.log("RewardSystem", "Doubled coins: $doubledCoins. Total: $totalCoins")
                callback(true)
            }
            override fun onAdFailed(type: RewardType, error: String) {
                Gdx.app.error("RewardSystem", "Ad failed: $error")
                callback(false)
            }
            override fun onAdClosed(type: RewardType, wasRewarded: Boolean) {
                if (!wasRewarded) {
                    totalCoins += currentRaceCoins
                }
            }
        })
    }
    fun requestInstantRepair(callback: (Boolean) -> Unit) {
        if (!adManager.isRewardedAdReady()) {
            callback(false)
            return
        }
        adManager.showRewardedAd(RewardType.INSTANT_REPAIR, object : AdRewardCallback {
            override fun onRewardEarned(type: RewardType, amount: Int) {
                Gdx.app.log("RewardSystem", "Instant repair granted")
                callback(true)
            }
            override fun onAdFailed(type: RewardType, error: String) {
                callback(false)
            }
            override fun onAdClosed(type: RewardType, wasRewarded: Boolean) {}
        })
    }
    fun requestPowerUp(type: RewardType, duration: Float, callback: (Boolean) -> Unit) {
        if (!adManager.isRewardedAdReady()) {
            callback(false)
            return
        }
        adManager.showRewardedAd(type, object : AdRewardCallback {
            override fun onRewardEarned(rewardType: RewardType, amount: Int) {
                activatePowerUp(rewardType, duration)
                Gdx.app.log("RewardSystem", "Power-up activated: $rewardType")
                callback(true)
            }
            override fun onAdFailed(rewardType: RewardType, error: String) {
                callback(false)
            }
            override fun onAdClosed(type: RewardType, wasRewarded: Boolean) {}
        })
    }
    private fun activatePowerUp(type: RewardType, duration: Float) {
        activePowerUps.add(type)
        powerUpTimers[type] = duration
    }
    fun update(delta: Float) {
        val expiredPowerUps = mutableListOf<RewardType>()
        powerUpTimers.forEach { (type, timeRemaining) ->
            val newTime = timeRemaining - delta
            if (newTime <= 0) {
                expiredPowerUps.add(type)
            } else {
                powerUpTimers[type] = newTime
            }
        }
        expiredPowerUps.forEach {
            deactivatePowerUp(it)
        }
    }
    private fun deactivatePowerUp(type: RewardType) {
        activePowerUps.remove(type)
        powerUpTimers.remove(type)
        Gdx.app.log("RewardSystem", "Power-up expired: $type")
    }
    fun isPowerUpActive(type: RewardType): Boolean = activePowerUps.contains(type)
    fun getPowerUpTimeRemaining(type: RewardType): Float = powerUpTimers[type] ?: 0f
    fun addCoins(amount: Int) {
        totalCoins += amount
    }
    fun spendCoins(amount: Int): Boolean {
        return if (totalCoins >= amount) {
            totalCoins -= amount
            true
        } else {
            false
        }
    }
    fun reset() {
        currentRaceCoins = 0
        pendingDoubleCoins = false
    }
}
