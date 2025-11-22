package com.bikeracing.game

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    private lateinit var adManager: AdManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adManager = AdManager(this)
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
        }
        val gameView = initializeForView(BikeRacingGame(adManager), config)
        val layout = RelativeLayout(this).apply {
            addView(gameView)
            val bannerParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
            val bannerView = adManager.createBannerAd()
            bannerView.visibility = View.GONE
            addView(bannerView, bannerParams)
        }
        setContentView(layout)
    }
    override fun onPause() {
        super.onPause()
        adManager.pause()
    }
    override fun onResume() {
        super.onResume()
        adManager.resume()
    }
    override fun onDestroy() {
        adManager.destroy()
        super.onDestroy()
    }
}
