package com.bikeracing.game

import com.badlogic.gdx.Game

class BikeRacingGame(private val adManager: AdManager) : Game() {
    private lateinit var rewardSystem: RewardSystem
    private lateinit var gameEconomy: GameEconomy
    override fun create() {
        adManager.initialize()
        rewardSystem = RewardSystem(adManager)
        gameEconomy = GameEconomy(rewardSystem)
        setScreen(MenuScreen(this, adManager, rewardSystem, gameEconomy))
    }
    override fun dispose() {
        super.dispose()
        adManager.destroy()
    }
}
