package com.bikeracing.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

enum class MenuState {
    MAIN, SHOP
}

class MenuScreen(
    private val game: BikeRacingGame,
    private val adManager: AdManager,
    private val rewardSystem: RewardSystem,
    private val gameEconomy: GameEconomy
) : Screen {
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val shapeRenderer = ShapeRenderer()
    private val bikeShop = BikeShop(gameEconomy, rewardSystem)
    private var menuState = MenuState.MAIN
    private val playButton = Rectangle()
    private val shopButton = Rectangle()
    private val backButton = Rectangle()
    private val doubleCoinsButton = Rectangle()
    private var showDoubleCoinsOffer = false
    init {
        adManager.showBanner()
        if (rewardSystem.currentRaceCoins > 0 && !rewardSystem.pendingDoubleCoins) {
            showDoubleCoinsOffer = true
        }
    }
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        when (menuState) {
            MenuState.MAIN -> renderMainMenu()
            MenuState.SHOP -> renderShopMenu()
        }
        handleInput()
    }
    private fun renderMainMenu() {
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(3f)
        font.draw(batch, "BIKE RACING", screenWidth / 2 - 200, screenHeight - 100)
        font.data.setScale(1.5f)
        font.color = Color.YELLOW
        font.draw(batch, "Coins: ${rewardSystem.totalCoins}", screenWidth / 2 - 100, screenHeight - 200)
        batch.end()
        renderMenuButtons(screenWidth, screenHeight)
        if (showDoubleCoinsOffer) {
            renderDoubleCoinsOffer(screenWidth, screenHeight)
        }
    }
    private fun renderMenuButtons(screenWidth: Float, screenHeight: Float) {
        val buttonWidth = 300f
        val buttonHeight = 80f
        val centerX = screenWidth / 2 - buttonWidth / 2
        playButton.set(centerX, screenHeight / 2 + 50, buttonWidth, buttonHeight)
        shopButton.set(centerX, screenHeight / 2 - 50, buttonWidth, buttonHeight)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.2f, 0.6f, 0.2f, 1f)
        shapeRenderer.rect(playButton.x, playButton.y, playButton.width, playButton.height)
        shapeRenderer.color = Color(0.2f, 0.4f, 0.6f, 1f)
        shapeRenderer.rect(shopButton.x, shopButton.y, shopButton.width, shopButton.height)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(2f)
        font.draw(batch, "PLAY", playButton.x + 100, playButton.y + 50)
        font.draw(batch, "BIKE SHOP", shopButton.x + 70, shopButton.y + 50)
        font.data.setScale(1f)
        batch.end()
    }
    private fun renderDoubleCoinsOffer(screenWidth: Float, screenHeight: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.8f)
        shapeRenderer.rect(screenWidth / 2 - 250, screenHeight / 2 - 100, 500f, 250f)
        shapeRenderer.end()
        doubleCoinsButton.set(screenWidth / 2 - 150, screenHeight / 2 - 50, 300f, 80f)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.8f, 0.6f, 0f, 1f)
        shapeRenderer.rect(doubleCoinsButton.x, doubleCoinsButton.y, doubleCoinsButton.width, doubleCoinsButton.height)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(batch, "Double Your Coins!", screenWidth / 2 - 120, screenHeight / 2 + 120)
        font.draw(batch, "Earned: ${rewardSystem.currentRaceCoins}", screenWidth / 2 - 100, screenHeight / 2 + 80)
        font.data.setScale(2f)
        font.draw(batch, "WATCH AD", doubleCoinsButton.x + 70, doubleCoinsButton.y + 50)
        font.data.setScale(1f)
        batch.end()
    }
    private fun renderShopMenu() {
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()
        bikeShop.render(batch, screenWidth, screenHeight)
        renderBackButton(screenWidth, screenHeight)
    }
    private fun renderBackButton(screenWidth: Float, screenHeight: Float) {
        backButton.set(20f, 20f, 150f, 60f)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.6f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(batch, "BACK", backButton.x + 35, backButton.y + 40)
        font.data.setScale(1f)
        batch.end()
    }
    private fun handleInput() {
        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()
            when (menuState) {
                MenuState.MAIN -> handleMainMenuTouch(touchX, touchY)
                MenuState.SHOP -> handleShopTouch(touchX, touchY)
            }
        }
    }
    private fun handleMainMenuTouch(x: Float, y: Float) {
        if (showDoubleCoinsOffer && doubleCoinsButton.contains(x, y)) {
            rewardSystem.requestDoubleCoinsReward { success ->
                if (success) {
                    showDoubleCoinsOffer = false
                    rewardSystem.reset()
                }
            }
            return
        }
        if (playButton.contains(x, y)) {
            adManager.hideBanner()
            showDoubleCoinsOffer = false
            rewardSystem.reset()
            game.screen = GameScreen(game, adManager, rewardSystem, gameEconomy)
        }
        if (shopButton.contains(x, y)) {
            menuState = MenuState.SHOP
        }
    }
    private fun handleShopTouch(x: Float, y: Float) {
        if (backButton.contains(x, y)) {
            menuState = MenuState.MAIN
            return
        }
        bikeShop.handleTouch(x, y)
    }
    override fun show() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {
        adManager.pause()
    }
    override fun resume() {
        adManager.resume()
    }
    override fun hide() {}
    override fun dispose() {
        batch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        bikeShop.dispose()
    }
}
