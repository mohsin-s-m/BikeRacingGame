package com.bikeracing.game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import kotlin.math.abs

class BikePhysics(private val world: World) {
    companion object {
        const val PPM = 100f
        private const val WHEEL_RADIUS = 0.4f
        private const val BIKE_DENSITY = 1.0f
        private const val WHEEL_FRICTION = 0.9f
        private const val MAX_MOTOR_SPEED = 50f
        private const val MOTOR_TORQUE = 80f
    }
    lateinit var bikeBody: Body
    lateinit var frontWheelBody: Body
    lateinit var rearWheelBody: Body
    private lateinit var frontWheelJoint: RevoluteJoint
    private lateinit var rearWheelJoint: RevoluteJoint
    var health = 100f
    var isDestroyed = false
    private var currentBikeStats = BikeStats()
    fun createBike(startX: Float, startY: Float, bikeStats: BikeStats) {
        currentBikeStats = bikeStats
        createBikeFrame(startX, startY)
        createWheels(startX, startY)
        attachWheels()
        health = 100f
        isDestroyed = false
    }
    private fun createBikeFrame(x: Float, y: Float) {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            angularDamping = 0.5f
            linearDamping = 0.3f
        }
        bikeBody = world.createBody(bodyDef)
        val shape = PolygonShape().apply {
            setAsBox(0.8f, 0.3f)
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = BIKE_DENSITY
            friction = 0.5f
            restitution = 0.1f
        }
        bikeBody.createFixture(fixtureDef)
        shape.dispose()
        bikeBody.userData = "bike"
    }
    private fun createWheels(x: Float, y: Float) {
        val wheelDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
        }
        wheelDef.position.set(x + 0.6f, y - 0.5f)
        frontWheelBody = world.createBody(wheelDef)
        wheelDef.position.set(x - 0.6f, y - 0.5f)
        rearWheelBody = world.createBody(wheelDef)
        val wheelShape = CircleShape().apply {
            radius = WHEEL_RADIUS
        }
        val wheelFixture = FixtureDef().apply {
            shape = wheelShape
            density = BIKE_DENSITY
            friction = WHEEL_FRICTION
            restitution = 0.3f
        }
        frontWheelBody.createFixture(wheelFixture)
        rearWheelBody.createFixture(wheelFixture)
        wheelShape.dispose()
        frontWheelBody.userData = "wheel"
        rearWheelBody.userData = "wheel"
    }
    private fun attachWheels() {
        val frontJointDef = RevoluteJointDef().apply {
            initialize(bikeBody, frontWheelBody, frontWheelBody.worldCenter)
            enableMotor = true
            maxMotorTorque = MOTOR_TORQUE * currentBikeStats.acceleration
        }
        frontWheelJoint = world.createJoint(frontJointDef) as RevoluteJoint
        val rearJointDef = RevoluteJointDef().apply {
            initialize(bikeBody, rearWheelBody, rearWheelBody.worldCenter)
            enableMotor = true
            maxMotorTorque = MOTOR_TORQUE * currentBikeStats.acceleration
        }
        rearWheelJoint = world.createJoint(rearJointDef) as RevoluteJoint
    }
    fun accelerate(speedMultiplier: Float = 1f) {
        val speed = MAX_MOTOR_SPEED * currentBikeStats.topSpeed * speedMultiplier
        frontWheelJoint.motorSpeed = -speed
        rearWheelJoint.motorSpeed = -speed
    }
    fun brake() {
        frontWheelJoint.motorSpeed = MAX_MOTOR_SPEED * 0.5f
        rearWheelJoint.motorSpeed = MAX_MOTOR_SPEED * 0.5f
    }
    fun lean(direction: Float) {
        val torque = 15f * direction * currentBikeStats.handling
        bikeBody.applyTorque(torque, true)
    }
    fun stopMotor() {
        frontWheelJoint.motorSpeed = 0f
        rearWheelJoint.motorSpeed = 0f
    }
    fun applyDamage(amount: Float) {
        if (isDestroyed) return
        health -= amount * (1f - currentBikeStats.durability * 0.1f)
        if (health <= 0) {
            health = 0f
            isDestroyed = true
        }
    }
    fun repair(amount: Float = 100f) {
        health = (health + amount).coerceAtMost(100f)
        if (health > 0) {
            isDestroyed = false
        }
    }
    fun getPosition(): Vector2 = bikeBody.position
    fun getAngle(): Float = bikeBody.angle
    fun getVelocity(): Vector2 = bikeBody.linearVelocity
    fun getSpeed(): Float = getVelocity().len()
    fun isBalanced(): Boolean {
        val angle = (getAngle() * 180f / Math.PI).toFloat()
        return abs(angle % 360) < 45f || abs(angle % 360) > 315f
    }
    fun performFlip() {
        bikeBody.applyAngularImpulse(8f, true)
    }
    fun dispose() {
        if (::bikeBody.isInitialized && bikeBody.fixtureList.size > 0) {
            world.destroyBody(frontWheelBody)
            world.destroyBody(rearWheelBody)
            world.destroyBody(bikeBody)
        }
    }
}

data class BikeStats(
    val topSpeed: Float = 1f,
    val acceleration: Float = 1f,
    val handling: Float = 1f,
    val durability: Float = 1f
)
