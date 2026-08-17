package com.mikazuki.pocketfamiliar.pet.physics

import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import kotlin.math.hypot
import kotlin.random.Random

private const val BASE_GRAVITY = 1400f
private const val MAX_FALL_SPEED = 2400f
private const val CLIMB_SPEED = 120f
private const val RUN_MULTIPLIER = 2.2f
private const val SETTLE_VERTICAL_SPEED = 150f

/** Lightweight game physics for the overlay familiar. */
class PetPhysicsEngine {

    var x: Float = 0f
    var y: Float = 0f
    var velocityX: Float = 0f
    var velocityY: Float = 0f

    var profile: FamiliarPhysicsProfile = FamiliarPhysicsProfile()
    var lastImpactSeverity: ImpactSeverity = ImpactSeverity.SOFT
        private set

    var screenWidth: Int = 1080
    var screenHeight: Int = 1920
    var petWidth: Int = 128
    var petHeight: Int = 128
    var bottomInsetPx: Int = 0

    private val climbCeilingFraction = 0.15f
    private val maxX get() = (screenWidth - petWidth).toFloat().coerceAtLeast(0f)
    private val maxY get() = (screenHeight - petHeight - bottomInsetPx).toFloat().coerceAtLeast(0f)
    private val climbCeiling get() = screenHeight * climbCeilingFraction

    fun update(currentState: PetState, deltaSeconds: Float, movementSpeed: Float): ForcedTransition? =
        when (currentState) {
            is PetState.WalkLeft -> updateWalk(-movementSpeed, deltaSeconds)
            is PetState.WalkRight -> updateWalk(movementSpeed, deltaSeconds)
            is PetState.RunLeft -> updateWalk(-movementSpeed * RUN_MULTIPLIER, deltaSeconds)
            is PetState.RunRight -> updateWalk(movementSpeed * RUN_MULTIPLIER, deltaSeconds)
            is PetState.ClimbLeft -> updateClimb(true, deltaSeconds)
            is PetState.ClimbRight -> updateClimb(false, deltaSeconds)
            is PetState.Falling,
            is PetState.Thrown,
            is PetState.Jumping -> updateAirborne(deltaSeconds)
            else -> null
        }

    private fun updateWalk(speed: Float, delta: Float): ForcedTransition? {
        x += speed * delta
        return when {
            x < 0f -> { x = 0f; decideEdgeReaction(true) }
            x > maxX -> { x = maxX; decideEdgeReaction(false) }
            else -> null
        }
    }

    private fun decideEdgeReaction(leftEdge: Boolean): ForcedTransition =
        if (Random.nextFloat() < 0.35f) {
            if (leftEdge) ForcedTransition.ClimbLeft else ForcedTransition.ClimbRight
        } else {
            if (leftEdge) ForcedTransition.TurnRight else ForcedTransition.TurnLeft
        }

    private fun updateClimb(onLeftWall: Boolean, delta: Float): ForcedTransition? {
        x = if (onLeftWall) 0f else maxX
        y -= CLIMB_SPEED * delta
        return if (y <= climbCeiling) {
            y = climbCeiling
            ForcedTransition.JumpOff
        } else null
    }

    private fun updateAirborne(delta: Float): ForcedTransition? {
        val gravity = BASE_GRAVITY * profile.gravityScale
        velocityY = (velocityY + gravity * delta).coerceAtMost(MAX_FALL_SPEED)

        val dragFactor = (1f - profile.airDrag * delta).coerceIn(0f, 1f)
        velocityX *= dragFactor
        velocityY *= (1f - profile.airDrag * 0.08f * delta).coerceIn(0f, 1f)

        x += velocityX * delta
        y += velocityY * delta

        if (x < 0f) {
            x = 0f
            velocityX = -velocityX * profile.restitution
        } else if (x > maxX) {
            x = maxX
            velocityX = -velocityX * profile.restitution
        }

        if (y >= maxY) {
            y = maxY
            val impactSpeed = kotlin.math.abs(velocityY)
            lastImpactSeverity = severityForImpact(impactSpeed)

            if (impactSpeed > SETTLE_VERTICAL_SPEED) {
                velocityY = -impactSpeed * profile.restitution
                velocityX *= profile.floorFriction

                if (kotlin.math.abs(velocityY) > SETTLE_VERTICAL_SPEED) {
                    return null
                }
            }

            velocityX = 0f
            velocityY = 0f
            return ForcedTransition.Land
        }

        return null
    }

    fun launchFromEdge(fromLeftWall: Boolean) {
        velocityX = if (fromLeftWall) 400f else -400f
        velocityY = -600f
    }

    /**
     * Seeds the airborne simulation from the user's release gesture. Lower-mass
     * familiars inherit more speed from the same finger movement; heavy ones feel
     * harder to yeet. Both axes are preserved, so upward and sideways throws arc.
     */
    fun onThrown(releaseVelocityX: Float, releaseVelocityY: Float) {
        val massScale = 1f / profile.mass.coerceAtLeast(0.25f)
        var vx = releaseVelocityX * massScale
        var vy = releaseVelocityY * massScale
        val speed = hypot(vx, vy)
        if (speed > profile.maxThrowSpeed && speed > 0f) {
            val scale = profile.maxThrowSpeed / speed
            vx *= scale
            vy *= scale
        }
        velocityX = vx
        velocityY = vy
    }

    private fun severityForImpact(speed: Float): ImpactSeverity = when {
        speed < 420f -> ImpactSeverity.SOFT
        speed < 900f -> ImpactSeverity.NORMAL
        speed < 1500f -> ImpactSeverity.HARD
        else -> ImpactSeverity.CATASTROPHIC
    }

    fun onScreenSizeChanged(newWidth: Int, newHeight: Int, newBottomInset: Int = bottomInsetPx) {
        screenWidth = newWidth
        screenHeight = newHeight
        bottomInsetPx = newBottomInset
        x = x.coerceIn(0f, maxX)
        y = y.coerceIn(0f, maxY)
    }
}

enum class ForcedTransition {
    TurnLeft,
    TurnRight,
    ClimbLeft,
    ClimbRight,
    JumpOff,
    Land,
}
