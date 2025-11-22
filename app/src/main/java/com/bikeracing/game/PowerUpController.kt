package com.bikeracing.game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array

data class PowerUpEffect(
    val type: RewardType,
    val duration: Float,
    var timeRemaining: Float,
    var isActive: Boolean = true
)

data class Coin(
    val position: Vector2,
    val value: Int = 10,
    var collected: Boolean = false
)

class PowerUpController(
    private val bikePhysics: BikePhysics,
    private val rewardSystem: RewardSystem
) {
    private val activePowerUps = Array<PowerUpEffect>()
    private val collectibleCoins = Array<Coin>()
    var speedBoostMultiplier = 1f
    var coinMagnetActive = false
    var shieldActive = false
    var timeSlowActive = false
    var timeDilation = 1f
    fun update(delta: Float) {
        val adjustedDelta = delta * timeDilation
        val iterator = activePowerUps.iterator()
        while (iterator.hasNext()) {
            val powerUp = iterator.next()
            powerUp.timeRemaining -= adjustedDelta
            if (powerUp.timeRemaining <= 0) {
                deactivatePowerUp(powerUp.type)
                iterator.remove()
            }
        }
        if (coinMagnetActive) {
            attractCoins()
        }
        rewardSystem.update(adjustedDelta)
    }
    fun activatePowerUp(type: RewardType, duration: Float) {
        val existingPowerUp = activePowerUps.firstOrNull { it.type == type }
        if (existingPowerUp != null) {
            existingPowerUp.timeRemaining = duration
            return
        }
        val powerUpEffect = PowerUpEffect(type, duration, duration)
        activePowerUps.add(powerUpEffect)
        when (type) {
            RewardType.SPEED_BOOST -> {
                speedBoostMultiplier = 1.5f
            }
            RewardType.COIN_MAGNET -> {
                coinMagnetActive = true
            }
            RewardType.SHIELD -> {
                shieldActive = true
            }
            RewardType.TIME_SLOW -> {
                timeSlowActive = true
                timeDilation = 0.7f
            }
            else -> {}
        }
    }
    private fun deactivatePowerUp(type: RewardType) {
        when (type) {
            RewardType.SPEED_BOOST -> {
                speedBoostMultiplier = 1f
            }
            RewardType.COIN_MAGNET -> {
                coinMagnetActive = false
            }
            RewardType.SHIELD -> {
                shieldActive = false
            }
            RewardType.TIME_SLOW -> {
                timeSlowActive = false
                timeDilation = 1f
            }
            else -> {}
        }
    }
    private fun attractCoins() {
        val bikePos = bikePhysics.getPosition()
        val magnetRange = 5f
        collectibleCoins.forEach { coin ->
            if (!coin.collected) {
                val distance = bikePos.dst(coin.position)
                if (distance < magnetRange) {
                    val direction = Vector2(bikePos).sub(coin.position).nor()
                    coin.position.add(direction.scl(10f * 0.016f))
                    if (distance < 0.5f) {
                        collectCoin(coin)
                    }
                }
            }
        }
    }
    private fun collectCoin(coin: Coin) {
        if (!coin.collected) {
            coin.collected = true
            rewardSystem.addCoins(coin.value)
        }
    }
    fun handleCollision(damage: Float) {
        if (shieldActive) {
            return
        }
        bikePhysics.applyDamage(damage)
    }
    fun addCoin(coin: Coin) {
        collectibleCoins.add(coin)
    }
    fun checkCoinCollection(bikePosition: Vector2, collectRadius: Float): Int {
        var coinsCollected = 0
        collectibleCoins.forEach { coin ->
            if (!coin.collected && bikePosition.dst(coin.position) < collectRadius) {
                collectCoin(coin)
                coinsCollected++
            }
        }
        return coinsCollected
    }
    fun getActiveEffects(): Array<PowerUpEffect> = activePowerUps
    fun isPowerUpActive(type: RewardType): Boolean {
        return activePowerUps.any { it.type == type && it.isActive }
    }
    fun getPowerUpTimeRemaining(type: RewardType): Float {
        return activePowerUps.firstOrNull { it.type == type }?.timeRemaining ?: 0f
    }
    fun reset() {
        activePowerUps.clear()
        speedBoostMultiplier = 1f
        coinMagnetActive = false
        shieldActive = false
        timeSlowActive = false
        timeDilation = 1f
        collectibleCoins.clear()
    }
}
