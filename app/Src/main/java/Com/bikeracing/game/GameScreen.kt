package com.bikeracing.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World

class GameScreen(
    private val game: BikeRacingGame,
    private val adManager: AdManager,
    private val rewardSystem: RewardSystem,
    private val gameEconomy: GameEconomy
) : Screen {
    private val world = World(Vector2(0f, -9.8f), true)
    private val camera = OrthographicCamera()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val shapeRenderer = ShapeRenderer()
    private val debugRenderer = Box2DDebugRenderer()
    private lateinit var bikePhysics: BikePhysics
    private lateinit var powerUpController: PowerUpController
    private lateinit var terrainGenerator: TerrainGenerator
    private var raceDistance = 0f
    private var raceTime = 0f
    private var coinsCollected = 0
    private var stuntsPerformed = 0
    private var isRaceComplete = false
    private var showPowerUpMenu = false
    init {
        camera.setToOrtho(false, 16f, 9f)
        setupGame()
    }
    private fun setupGame() {
        bikePhysics = BikePhysics(world)
        powerUpController = PowerUpController(bikePhysics, rewardSystem)
        terrainGenerator = TerrainGenerator(world)
        val bikeStats = gameEconomy.getCurrentBikeStats()
        bikePhysics.createBike(2f, 5f, bikeStats)
        terrainGenerator.generateInitialTerrain()
        spawnCoins()
    }
    private fun spawnCoins() {
        for (i in 0..50) {
            val x = 5f + i * 2f + (Math.random() * 2f).toFloat()
            val y = 3f + (Math.random() * 3f).toFloat()
            powerUpController.addCoin(Coin(Vector2(x, y), 10))
        }
    }
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.5f, 0.7f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        if (!isRaceComplete) {
            handleInput(delta)
            updateGame(delta)
        }
        updateCamera()
        renderGame()
        renderUI()
        if (showPowerUpMenu) {
            renderPowerUpMenu()
        }
    }
    private fun handleInput(delta: Float) {
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            bikePhysics.accelerate(powerUpController.speedBoostMultiplier)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            bikePhysics.brake()
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            bikePhysics.lean(-1f)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            bikePhysics.lean(1f)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            bikePhysics.performFlip()
            stuntsPerformed++
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            showPowerUpMenu = !showPowerUpMenu
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            requestPowerUp(RewardType.SPEED_BOOST)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            requestPowerUp(RewardType.COIN_MAGNET)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            requestPowerUp(RewardType.TIME_SLOW)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            requestPowerUp(RewardType.SHIELD)
        }
        if (bikePhysics.health < 30f && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            requestInstantRepair()
        }
    }
    private fun requestPowerUp(type: RewardType) {
        val duration = when (type) {
            RewardType.SPEED_BOOST -> 30f
            RewardType.COIN_MAGNET -> 30f
            RewardType.TIME_SLOW -> 15f
            RewardType.SHIELD -> 20f
            else -> 30f
        }
        rewardSystem.requestPowerUp(type, duration) { success ->
            if (success) {
                powerUpController.activatePowerUp(type, duration)
            }
        }
    }
    private fun requestInstantRepair() {
        rewardSystem.requestInstantRepair { success ->
            if (success) {
                bikePhysics.repair(100f)
            }
        }
    }
    private fun updateGame(delta: Float) {
        val adjustedDelta = delta * powerUpController.timeDilation
        world.step(adjustedDelta, 6, 2)
        powerUpController.update(adjustedDelta)
        raceTime += adjustedDelta
        val bikePos = bikePhysics.getPosition()
        raceDistance = bikePos.x
        val collected = powerUpController.checkCoinCollection(bikePos, 0.5f)
        coinsCollected += collected
        terrainGenerator.update(bikePos.x)
        if (bikePhysics.isDestroyed) {
            completeRace()
        }
        if (raceDistance > 100f) {
            completeRace()
        }
    }
    private fun completeRace() {
        if (isRaceComplete) return
        isRaceComplete = true
        val timeBonus = (300f - raceTime).coerceAtLeast(0f)
        val totalReward = gameEconomy.calculateRaceReward(raceDistance, timeBonus, stuntsPerformed)
        rewardSystem.onRaceComplete(totalReward)
        adManager.showInterstitialAd()
    }
    private fun updateCamera() {
        val bikePos = bikePhysics.getPosition()
        camera.position.set(bikePos.x + 3f, bikePos.y, 0f)
        camera.update()
    }
    private fun renderGame() {
        batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.2f, 0.8f, 0.2f, 1f)
        shapeRenderer.rect(-10f, -1f, 200f, 1f)
        shapeRenderer.end()
        debugRenderer.render(world, camera.combined)
        batch.begin()
        powerUpController.collectibleCoins.forEach { coin ->
            if (!coin.collected) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.color = Color.YELLOW
                shapeRenderer.circle(coin.position.x, coin.position.y, 0.2f)
                shapeRenderer.end()
            }
        }
        batch.end()
    }
    private fun renderUI() {
        val uiCamera = OrthographicCamera()
        uiCamera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        batch.projectionMatrix = uiCamera.combined
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(batch, "Distance: ${raceDistance.toInt()}m", 20f, Gdx.graphics.height - 20f)
        font.draw(batch, "Time: ${raceTime.toInt()}s", 20f, Gdx.graphics.height - 50f)
        font.draw(batch, "Coins: $coinsCollected", 20f, Gdx.graphics.height - 80f)
        font.draw(batch, "Health: ${bikePhysics.health.toInt()}", 20f, Gdx.graphics.height - 110f)
        font.draw(batch, "Stunts: $stuntsPerformed", 20f, Gdx.graphics.height - 140f)
        renderActiveEffects()
        if (isRaceComplete) {
            renderRaceComplete()
        }
        font.data.setScale(1f)
        batch.end()
    }
    private fun renderActiveEffects() {
        var yOffset = 200f
        powerUpController.getActiveEffects().forEach { effect ->
            font.color = Color.CYAN
            font.draw(batch, "${effect.type.name}: ${effect.timeRemaining.toInt()}s", 20f, Gdx.graphics.height - yOffset)
            yOffset += 30f
        }
    }
    private fun renderRaceComplete() {
        font.color = Color.YELLOW
        font.data.setScale(2f)
        font.draw(batch, "RACE COMPLETE!", Gdx.graphics.width / 2f - 150, Gdx.graphics.height / 2f)
        font.data.setScale(1.5f)
        font.draw(batch, "Press ENTER to continue", Gdx.graphics.width / 2f - 150, Gdx.graphics.height / 2f - 50)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.screen = MenuScreen(game, adManager, rewardSystem, gameEconomy)
        }
    }
    private fun renderPowerUpMenu() {
        shapeRenderer.projectionMatrix = batch.projectionMatrix
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        shapeRenderer.rect(Gdx.graphics.width / 2f - 200, Gdx.graphics.height / 2f - 150, 400f, 300f)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        font.draw(batch, "POWER-UPS (Watch Ad)", Gdx.graphics.width / 2f - 120, Gdx.graphics.height / 2f + 120)
        font.data.setScale(1f)
        font.draw(batch, "1 - Speed Boost (30s)", Gdx.graphics.width / 2f - 100, Gdx.graphics.height / 2f + 80)
        font.draw(batch, "2 - Coin Magnet (30s)", Gdx.graphics.width / 2f - 100, Gdx.graphics.height / 2f + 50)
        font.draw(batch, "3 - Time Slow (15s)", Gdx.graphics.width / 2f - 100, Gdx.graphics.height / 2f + 20)
        font.draw(batch, "4 - Shield (20s)", Gdx.graphics.width / 2f - 100, Gdx.graphics.height / 2f - 10)
        font.draw(batch, "R - Instant Repair", Gdx.graphics.width / 2f - 100, Gdx.graphics.height / 2f - 40)
        font.draw(batch, "P - Close Menu", Gdx.graphics.width / 2f - 100, Gdx.graphics.height / 2f - 100)
        batch.end()
    }
    override fun show() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        world.dispose()
        batch.dispose()
        font.dispose()
        shapeRenderer.dispose()
        debugRenderer.dispose()
    }
}
