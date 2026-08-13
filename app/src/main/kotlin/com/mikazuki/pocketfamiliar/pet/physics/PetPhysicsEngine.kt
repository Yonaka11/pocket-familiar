package com.mikazuki.pocketfamiliar.pet.physics

import com.mikazuki.pocketfamiliar.pet.behavior.PetState

private const val GRAVITY = 1400f       // px/s² — snappy but not instant
private const val MAX_FALL_SPEED = 2000f

/**
 * Lightweight physics for the pet overlay.
 *
 * Operates in raw screen-pixel coordinates, matching [WindowManager.LayoutParams]
 * x/y which are relative to the full display (including system bars when
 * [FLAG_LAYOUT_IN_SCREEN] is set).
 *
 * Screen dimensions should be updated via [onScreenSizeChanged] whenever the
 * display configuration changes (rotation, multi-window, etc.).
 */
class PetPhysicsEngine {

    var x: Float = 0f
    var y: Float = 0f
    var velocityX: Float = 0f
    var velocityY: Float = 0f

    var screenWidth: Int = 1080
    var screenHeight: Int = 1920

    var petWidth: Int = 128
    var petHeight: Int = 128

    /**
     * Bottom inset in pixels (navigation bar height).
     * The pet lands on top of the navigation bar so it stays in the tappable
     * region. Set from [WindowInsets] by the service.
     */
    var bottomInsetPx: Int = 0

    private val maxX get() = (screenWidth - petWidth).toFloat().coerceAtLeast(0f)
    private val maxY get() = (screenHeight - petHeight - bottomInsetPx).toFloat().coerceAtLeast(0f)

    /**
     * Advance physics by [deltaSeconds] for the current [state].
     * Returns a [ForcedTransition] when a boundary event requires a state change,
     * or null if no transition is needed.
     */
    fun update(currentState: PetState, deltaSeconds: Float, movementSpeed: Float): ForcedTransition? {
        return when (currentState) {
            is PetState.WalkLeft -> updateWalk(-movementSpeed, deltaSeconds)
            is PetState.WalkRight -> updateWalk(movementSpeed, deltaSeconds)
            is PetState.Falling -> updateFalling(deltaSeconds)
            // Other states do not move autonomously; no physics update needed.
            else -> null
        }
    }

    private fun updateWalk(speed: Float, delta: Float): ForcedTransition? {
        x += speed * delta
        return when {
            x < 0f -> {
                x = 0f
                ForcedTransition.TurnRight
            }
            x > maxX -> {
                x = maxX
                ForcedTransition.TurnLeft
            }
            else -> null
        }
    }

    private fun updateFalling(delta: Float): ForcedTransition? {
        velocityY = (velocityY + GRAVITY * delta).coerceAtMost(MAX_FALL_SPEED)
        y += velocityY * delta
        x = x.coerceIn(0f, maxX)

        return if (y >= maxY) {
            y = maxY
            velocityY = 0f
            ForcedTransition.Land
        } else null
    }

    /**
     * Position the pet at the drag coordinates, clamped to valid screen bounds.
     * Called by [PetOverlayManager] during an ongoing drag.
     */
    fun applyDragPosition(rawX: Float, rawY: Float) {
        x = rawX.coerceIn(0f, maxX)
        y = rawY.coerceIn(0f, maxY)
    }

    /**
     * Called when the user releases the pet.
     * [releaseVelocityY] is the finger's downward speed in px/s at the moment of
     * release; a positive value gives the initial falling impulse.
     */
    fun onDragReleased(releaseVelocityY: Float = 0f) {
        velocityX = 0f
        velocityY = releaseVelocityY.coerceIn(0f, MAX_FALL_SPEED)
    }

    /**
     * Update screen dimensions and re-clamp the pet to the new valid region.
     * Safe to call at any time, including mid-animation.
     */
    fun onScreenSizeChanged(newWidth: Int, newHeight: Int, newBottomInset: Int = bottomInsetPx) {
        screenWidth = newWidth
        screenHeight = newHeight
        bottomInsetPx = newBottomInset
        x = x.coerceIn(0f, maxX)
        y = y.coerceIn(0f, maxY)
    }
}

enum class ForcedTransition {
    TurnLeft, TurnRight, Land
}
