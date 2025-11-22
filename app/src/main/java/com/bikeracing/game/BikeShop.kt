package com.bikeracing.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class BikeShop(
    private val gameEconomy: GameEconomy,
    private val rewardSystem: RewardSystem
) {
    private val shapeRenderer = ShapeRenderer()
    private val font = BitmapFont()
    private var selectedBikeIndex = 0
    private val bikeButtons = mutableListOf<Rectangle>()
    private val upgradeButtons = mutableMapOf<String, Rectangle>()
    private val unlockButton = Rectangle()
    fun render(batch: SpriteBatch, screenWidth: Float, screenHeight: Float) {
        shapeRenderer.projectionMatrix = batch.projectionMatrix
        renderBackground()
        batch.begin()
        renderHeader(batch, screenWidth)
        renderBikeList(batch, screenWidth, screenHeight)
        renderBikeDetails(batch, screenWidth, screenHeight)
        renderCoinBalance(batch, screenWidth, screenHeight)
        batch.end()
    }
    private fun renderBackground() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.15f, 0.15f, 0.2f, 1f)
        shapeRenderer.rect(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        shapeRenderer.end()
    }
    private fun renderHeader(batch: SpriteBatch, screenWidth: Float) {
        font.color = Color.WHITE
        font.data.setScale(2f)
        font.draw(batch, "BIKE SHOP", screenWidth / 2 - 100, Gdx.graphics.height - 50f)
        font.data.setScale(1f)
    }
    private fun renderBikeList(batch: SpriteBatch, screenWidth: Float, screenHeight: Float) {
        bikeButtons.clear()
        val startY = screenHeight - 150
        val buttonHeight = 80f
        val buttonWidth = 250f
        gameEconomy.bikes.forEachIndexed { index, bike ->
            val y = startY - index * (buttonHeight + 10)
            val rect = Rectangle(20f, y, buttonWidth, buttonHeight)
            bikeButtons.add(rect)
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            if (index == selectedBikeIndex) {
                shapeRenderer.color = Color(0.3f, 0.6f, 0.9f, 1f)
            } else if (bike.isUnlocked) {
                shapeRenderer.color = Color(0.2f, 0.4f, 0.6f, 1f)
            } else {
                shapeRenderer.color = Color(0.3f, 0.3f, 0.3f, 1f)
            }
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
            shapeRenderer.end()
            font.color = Color.WHITE
            font.draw(batch, bike.name, rect.x + 10, rect.y + 50)
            if (bike.isUnlocked) {
                font.color = Color.GREEN
                font.draw(batch, "OWNED", rect.x + 10, rect.y + 25)
            } else {
                font.color = Color.YELLOW
                font.draw(batch, "${bike.price} coins", rect.x + 10, rect.y + 25)
            }
        }
    }
    private fun renderBikeDetails(batch: SpriteBatch, screenWidth: Float, screenHeight: Float) {
        val bike = gameEconomy.bikes[selectedBikeIndex]
        val detailsX = 300f
        val detailsY = screenHeight - 200
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(batch, bike.name, detailsX, detailsY)
        font.data.setScale(1f)
        val stats = if (bike.isUnlocked) {
            gameEconomy.getCurrentBikeStats()
        } else {
            bike.stats
        }
        val statsY = detailsY - 50
        font.draw(batch, "Speed: ${String.format("%.1f", stats.topSpeed)}", detailsX, statsY)
        font.draw(batch, "Acceleration: ${String.format("%.1f", stats.acceleration)}", detailsX, statsY - 30)
        font.draw(batch, "Handling: ${String.format("%.1f", stats.handling)}", detailsX, statsY - 60)
        font.draw(batch, "Durability: ${String.format("%.1f", stats.durability)}", detailsX, statsY - 90)
        if (bike.isUnlocked) {
            renderUpgradeButtons(batch, detailsX, statsY - 150)
        } else {
            renderUnlockButton(batch, bike, detailsX, statsY - 150)
        }
    }
    private fun renderUpgradeButtons(batch: SpriteBatch, x: Float, y: Float) {
        upgradeButtons.clear()
        val upgrades = listOf("speed", "acceleration", "handling", "durability")
        upgrades.forEachIndexed { index, upgradeType ->
            val buttonY = y - index * 50f
            val rect = Rectangle(x, buttonY, 200f, 40f)
            upgradeButtons[upgradeType] = rect
            val currentLevel = gameEconomy.getUpgradeLevel(selectedBikeIndex, upgradeType)
            val cost = gameEconomy.getNextUpgradeCost(selectedBikeIndex, upgradeType)
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            if (currentLevel >= 5) {
                shapeRenderer.color = Color.GRAY
            } else {
                shapeRenderer.color = Color(0.2f, 0.5f, 0.3f, 1f)
            }
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
            shapeRenderer.end()
            font.color = Color.WHITE
            val levelText = "Lv$currentLevel"
            val costText = if (cost > 0) "$cost" else "MAX"
            font.draw(batch, "Upgrade $upgradeType", rect.x + 5, rect.y + 25)
            font.draw(batch, "$levelText - $costText", rect.x + 5, rect.y + 10)
        }
    }
    private fun renderUnlockButton(batch: SpriteBatch, bike: BikeData, x: Float, y: Float) {
        unlockButton.set(x, y, 200f, 50f)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        if (rewardSystem.totalCoins >= bike.price) {
            shapeRenderer.color = Color(0.3f, 0.7f, 0.3f, 1f)
        } else {
            shapeRenderer.color = Color.GRAY
        }
        shapeRenderer.rect(unlockButton.x, unlockButton.y, unlockButton.width, unlockButton.height)
        shapeRenderer.end()
        font.color = Color.WHITE
        font.draw(batch, "UNLOCK", unlockButton.x + 60, unlockButton.y + 35)
        font.draw(batch, "${bike.price} coins", unlockButton.x + 50, unlockButton.y + 15)
    }
    private fun renderCoinBalance(batch: SpriteBatch, screenWidth: Float, screenHeight: Float) {
        font.color = Color.YELLOW
        font.data.setScale(1.5f)
        font.draw(batch, "Coins: ${rewardSystem.totalCoins}", screenWidth - 200, screenHeight - 30)
        font.data.setScale(1f)
    }
    fun handleTouch(x: Float, y: Float): Boolean {
        bikeButtons.forEachIndexed { index, rect ->
            if (rect.contains(x, y)) {
                selectedBikeIndex = index
                return true
            }
        }
        val bike = gameEconomy.bikes[selectedBikeIndex]
        if (!bike.isUnlocked && unlockButton.contains(x, y)) {
            return gameEconomy.unlockBike(selectedBikeIndex)
        }
        if (bike.isUnlocked) {
            upgradeButtons.forEach { (upgradeType, rect) ->
                if (rect.contains(x, y)) {
                    return gameEconomy.purchaseUpgrade(selectedBikeIndex, upgradeType)
                }
            }
        }
        return false
    }
    fun dispose() {
        shapeRenderer.dispose()
        font.dispose()
    }
}
