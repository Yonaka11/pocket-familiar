package com.mikazuki.pocketfamiliar.pet.physics

import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import kotlin.random.Random

private const val GRAVITY        = 1400f     // px/s²
private const val MAX_FALL_SPEED = 2000f
private const val CLIMB_SPEED    = 120f      // px/s upward
private const val RUN_MULTIPLIER = 2.2f      // run is this much faster than walk

/**
 * Lightweight, delta-time physics for the pet overlay.
 *
 * Coordinates are in raw screen pixels, matching [WindowManager.LayoutParams] x/y.
 *
 * New in this version:
 *  - [velocityX] is applied during [PetState.Falling] and [PetState.Jumping] for
 *    arcing jumps off wall edges.
 *  - [updateClimb] moves the pet up the left/right edge and returns
 *    [ForcedTransition.JumpOff] when the pet reaches the top climb boundary.
 *  - [launchFromEdge] seeds [velocityX] and [velocityY] for the wall-launch arc.
 */
class PetPhysicsEngine {

    var x: Float = 0f
    var y: Float = 0f
    var velocityX: Float = 0f
    var velocityY: Float = 0f

    var screenWidth: Int  = 1080
    var screenHeight: Int = 1920
    var petWidth: Int     = 128
    var petHeight: Int    = 128
    var bottomInsetPx: Int = 0

    // How high the pet can climb before jumping off (fraction of screen height).
    // 0.15 = top 15 % of screen.
    private val climbCeilingFraction = 0.15f

    private val maxX get() = (screenWidth - petWidth).toFloat().coerceAtLeast(0f)
    private val maxY get() = (screenHeight - petHeight - bottomInsetPx).toFloat().coerceAtLeast(0f)
    private val climbCeiling get() = screenHeight * climbCeilingFraction

    // ── Main update ──────────────────────────────────────────────────────────

    fun update(currentState: PetState, deltaSeconds: Float, movementSpeed: Float): ForcedTransition? =
        when (currentState) {
            is PetState.WalkLeft  -> updateWalk(-movementSpeed, deltaSeconds)
            is PetState.WalkRight -> updateWalk( movementSpeed, deltaSeconds)
            is PetState.RunLeft   -> updateWalk(-movementSpeed * RUN_MULTIPLIER, deltaSeconds)
            is PetState.RunRight  -> updateWalk( movementSpeed * RUN_MULTIPLIER, deltaSeconds)
            is PetState.ClimbLeft  -> updateClimb(onLeftWall = true,  delta = deltaSeconds)
            is PetState.ClimbRight -> updateClimb(onLeftWall = false, delta = deltaSeconds)
            is PetState.Falling,
            is PetState.Jumping   -> updateFalling(deltaSeconds)
            else -> null
        }

    // ── Walk / run ───────────────────────────────────────────────────────────

    private fun updateWalk(speed: Float, delta: Float): ForcedTransition? {
        x += speed * delta
        return when {
            x < 0f  -> { x = 0f;   decideEdgeReaction(leftEdge = true)  }
            x > maxX -> { x = maxX; decideEdgeReaction(leftEdge = false) }
            else -> null
        }
    }

    /**
     * When the pet hits a screen edge during walking/running:
     *  - 35 % chance → climb up that edge
     *  - 65 % chance → simply turn around
     */
    private fun decideEdgeReaction(leftEdge: Boolean): ForcedTransition =
        if (Random.nextFloat() < 0.35f) {
            if (leftEdge) ForcedTransition.ClimbLeft else ForcedTransition.ClimbRight
        } else {
            if (leftEdge) ForcedTransition.TurnRight else ForcedTransition.TurnLeft
        }

    // ── Climb ────────────────────────────────────────────────────────────────

    private fun updateClimb(onLeftWall: Boolean, delta: Float): ForcedTransition? {
        // Keep pinned to the wall
        x = if (onLeftWall) 0f else maxX
        y -= CLIMB_SPEED * delta

        return when {
            y <= climbCeiling -> {
                y = climbCeiling
                ForcedTransition.JumpOff   // state machine will transition to Jumping
            }
            else -> null
        }
    }

    // ── Fall / jump arc ──────────────────────────────────────────────────────

    private fun updateFalling(delta: Float): ForcedTransition? {
        velocityY = (velocityY + GRAVITY * delta).coerceAtMost(MAX_FALL_SPEED)
        y += velocityY * delta
        x += velocityX * delta

        // Horizontal friction so the pet doesn't slide forever
        velocityX *= (1f - 3f * delta).coerceAtLeast(0f)

        x = x.coerceIn(0f, maxX)

        return if (y >= maxY) {
            y = maxY
            velocityX = 0f
            velocityY = 0f
            ForcedTransition.Land
        } else null
    }

    /**
     * Launch the pet off the wall it was just climbing.
     * [fromLeftWall] determines the horizontal direction (toward center).
     */
    fun launchFromEdge(fromLeftWall: Boolean) {
        velocityX = if (fromLeftWall) 400f else -400f   // px/s toward center
        velocityY = -600f                                // upward impulse, then gravity
    }

    // ── Drag ─────────────────────────────────────────────────────────────────

    fun onDragReleased(releaseVelocityY: Float = 0f) {
        velocityX = 0f
        velocityY = releaseVelocityY.coerceIn(0f, MAX_FALL_SPEED)
    }

    // ── Screen resize ────────────────────────────────────────────────────────

    fun onScreenSizeChanged(newWidth: Int, newHeight: Int, newBottomInset: Int = bottomInsetPx) {
        screenWidth  = newWidth
        screenHeight = newHeight
        bottomInsetPx = newBottomInset
        x = x.coerceIn(0f, maxX)
        y = y.coerceIn(0f, maxY)
    }
}

enum class ForcedTransition {
    TurnLeft, TurnRight,    // simple direction change
    ClimbLeft, ClimbRight,  // start climbing an edge
    JumpOff,                // launch from wall after climbing
    Land,                   // hit the floor after falling/jumping
}
