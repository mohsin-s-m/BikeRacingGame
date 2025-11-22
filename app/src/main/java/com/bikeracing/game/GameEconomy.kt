package com.bikeracing.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

data class BikeData(
    val id: Int,
    val name: String,
    val price: Int,
    val stats: BikeStats,
    var isUnlocked: Boolean = false
)

class GameEconomy(private val rewardSystem: RewardSystem) {
    private val prefs: Preferences = Gdx.app.getPreferences("BikeRacingPrefs")
    var currentBikeId: Int
        get() = prefs.getInteger("currentBike", 0)
        set(value) {
            prefs.putInteger("currentBike", value)
            prefs.flush()
        }
    val bikes = listOf(
        BikeData(
            0, "Starter",
            0,
            BikeStats(1f, 1f, 1f, 1f),
            true
        ),
        BikeData(
            1, "Racer",
            5000,
            BikeStats(1.2f, 1.1f, 1.15f, 1.05f)
        ),
        BikeData(
            2, "Pro",
            15000,
            BikeStats(1.4f, 1.3f, 1.25f, 1.15f)
        ),
        BikeData(
            3, "Champion",
            35000,
            BikeStats(1.6f, 1.5f, 1.4f, 1.25f)
        ),
        BikeData(
            4, "Legend",
            75000,
            BikeStats(2f, 1.8f, 1.6f, 1.5f)
        )
    )
    private val upgradeLevels = mutableMapOf<String, Int>()
    init {
        loadUnlockedBikes()
        loadUpgradeLevels()
    }
    private fun loadUnlockedBikes() {
        bikes.forEach { bike ->
            bike.isUnlocked = prefs.getBoolean("bike_${bike.id}_unlocked", bike.id == 0)
        }
    }
    private fun loadUpgradeLevels() {
        bikes.forEach { bike ->
            upgradeLevels["${bike.id}_speed"] = prefs.getInteger("${bike.id}_speed", 0)
            upgradeLevels["${bike.id}_acceleration"] = prefs.getInteger("${bike.id}_acceleration", 0)
            upgradeLevels["${bike.id}_handling"] = prefs.getInteger("${bike.id}_handling", 0)
            upgradeLevels["${bike.id}_durability"] = prefs.getInteger("${bike.id}_durability", 0)
        }
    }
    fun unlockBike(bikeId: Int): Boolean {
        val bike = bikes.getOrNull(bikeId) ?: return false
        if (bike.isUnlocked) return false
        return if (rewardSystem.spendCoins(bike.price)) {
            bike.isUnlocked = true
            prefs.putBoolean("bike_${bike.id}_unlocked", true)
            prefs.flush()
            Gdx.app.log("GameEconomy", "Unlocked bike: ${bike.name}")
            true
        } else {
            Gdx.app.log("GameEconomy", "Not enough coins to unlock ${bike.name}")
            false
        }
    }
    fun purchaseUpgrade(bikeId: Int, upgradeType: String): Boolean {
        val bike = bikes.getOrNull(bikeId) ?: return false
        if (!bike.isUnlocked) return false
        val currentLevel = getUpgradeLevel(bikeId, upgradeType)
        if (currentLevel >= 5) return false
        val cost = calculateUpgradeCost(currentLevel)
        return if (rewardSystem.spendCoins(cost)) {
            upgradeLevels["${bikeId}_$upgradeType"] = currentLevel + 1
            prefs.putInteger("${bikeId}_$upgradeType", currentLevel + 1)
            prefs.flush()
            Gdx.app.log("GameEconomy", "Upgraded $upgradeType for ${bike.name} to level ${currentLevel + 1}")
            true
        } else {
            false
        }
    }
    private fun calculateUpgradeCost(currentLevel: Int): Int {
        return when (currentLevel) {
            0 -> 1000
            1 -> 2500
            2 -> 5000
            3 -> 10000
            4 -> 20000
            else -> 50000
        }
    }
    fun getUpgradeLevel(bikeId: Int, upgradeType: String): Int {
        return upgradeLevels["${bikeId}_$upgradeType"] ?: 0
    }
    fun getCurrentBikeStats(): BikeStats {
        val bike = bikes[currentBikeId]
        val speedLevel = getUpgradeLevel(currentBikeId, "speed")
        val accelLevel = getUpgradeLevel(currentBikeId, "acceleration")
        val handleLevel = getUpgradeLevel(currentBikeId, "handling")
        val durabilityLevel = getUpgradeLevel(currentBikeId, "durability")
        return BikeStats(
            bike.stats.topSpeed + speedLevel * 0.1f,
            bike.stats.acceleration + accelLevel * 0.1f,
            bike.stats.handling + handleLevel * 0.1f,
            bike.stats.durability + durabilityLevel * 0.1f
        )
    }
    fun getCurrentBike(): BikeData = bikes[currentBikeId]
    fun selectBike(bikeId: Int): Boolean {
        val bike = bikes.getOrNull(bikeId) ?: return false
        return if (bike.isUnlocked) {
            currentBikeId = bikeId
            Gdx.app.log("GameEconomy", "Selected bike: ${bike.name}")
            true
        } else {
            false
        }
    }
    fun getNextUpgradeCost(bikeId: Int, upgradeType: String): Int {
        val currentLevel = getUpgradeLevel(bikeId, upgradeType)
        return if (currentLevel >= 5) -1 else calculateUpgradeCost(currentLevel)
    }
    fun calculateRaceReward(distance: Float, timeBonus: Float, stuntsPerformed: Int): Int {
        val baseReward = (distance * 10).toInt()
        val timeReward = (timeBonus * 50).toInt()
        val stuntReward = stuntsPerformed * 100
        return baseReward + timeReward + stuntReward
    }
}
