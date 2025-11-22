package com.bikeracing.game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import kotlin.math.sin

class TerrainGenerator(private val world: World) {
    private val terrainSegments = mutableListOf<Body>()
    private var lastSegmentX = 0f
    private val segmentLength = 5f
    private val maxSegments = 15
    fun generateInitialTerrain() {
        for (i in 0..10) {
            createTerrainSegment(i * segmentLength, 0f)
        }
    }
    fun update(bikeX: Float) {
        removeOldSegments(bikeX)
        generateNewSegments(bikeX)
    }
    private fun removeOldSegments(bikeX: Float) {
        val segmentsToRemove = terrainSegments.filter { body ->
            body.position.x < bikeX - 20f
        }
        segmentsToRemove.forEach { body ->
            world.destroyBody(body)
            terrainSegments.remove(body)
        }
    }
    private fun generateNewSegments(bikeX: Float) {
        while (lastSegmentX < bikeX + 50f) {
            val heightVariation = sin(lastSegmentX * 0.1f) * 2f
            createTerrainSegment(lastSegmentX + segmentLength, heightVariation)
            lastSegmentX += segmentLength
        }
    }
    private fun createTerrainSegment(x: Float, heightOffset: Float) {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(x, -0.5f + heightOffset)
        }
        val body = world.createBody(bodyDef)
        val vertices = arrayOf(
            Vector2(0f, 0f),
            Vector2(segmentLength, 0f),
            Vector2(segmentLength, -2f),
            Vector2(0f, -2f)
        )
        val shape = ChainShape().apply {
            createLoop(vertices)
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            friction = 0.8f
            restitution = 0.1f
        }
        body.createFixture(fixtureDef)
        shape.dispose()
        terrainSegments.add(body)
        if ((x / segmentLength).toInt() % 3 == 0) {
            createObstacle(x + segmentLength / 2, heightOffset + 0.5f)
        }
    }
    private fun createObstacle(x: Float, y: Float) {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(x, y)
        }
        val body = world.createBody(bodyDef)
        val shape = PolygonShape().apply {
            setAsBox(0.3f, 0.3f)
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            friction = 0.5f
            restitution = 0.2f
        }
        body.createFixture(fixtureDef)
        shape.dispose()
        body.userData = "obstacle"
        terrainSegments.add(body)
    }
    fun dispose() {
        terrainSegments.forEach { world.destroyBody(it) }
        terrainSegments.clear()
    }
