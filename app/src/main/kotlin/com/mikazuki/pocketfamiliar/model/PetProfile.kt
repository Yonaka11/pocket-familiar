package com.mikazuki.pocketfamiliar.model

import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.behavior.PetState

/**
 * Defines one selectable pet character — its display info and all animations.
 *
 * [walkAnim] is reused for both WalkLeft and WalkRight; [PetView] horizontally
 * flips the canvas for left-facing states so we don't need duplicate art.
 * Same principle applies to [runAnim] and [climbAnim].
 */
data class PetProfile(
    val id: String,
    val displayName: String,
    val description: String,
    val previewResId: Int,          // drawable shown in the home-screen pet card
    val idleAnim: PetAnimation,
    val walkAnim: PetAnimation,
    val runAnim: PetAnimation,
    val sleepAnim: PetAnimation,
    val fallAnim: PetAnimation,
    val climbAnim: PetAnimation,
    val jumpAnim: PetAnimation,
) {
    fun animationForState(state: PetState): PetAnimation = when (state) {
        is PetState.Idle            -> idleAnim
        is PetState.WalkLeft,
        is PetState.WalkRight       -> walkAnim
        is PetState.RunLeft,
        is PetState.RunRight        -> runAnim
        is PetState.Sleep           -> sleepAnim
        is PetState.Falling         -> fallAnim
        is PetState.Dragged         -> fallAnim
        is PetState.ClimbLeft,
        is PetState.ClimbRight      -> climbAnim
        is PetState.Jumping         -> jumpAnim
    }

    /**
     * Returns true when the sprite should be drawn mirrored horizontally.
     * All left-facing states flip the right-facing art.
     */
    fun isFlippedForState(state: PetState): Boolean = when (state) {
        is PetState.WalkLeft,
        is PetState.RunLeft,
        is PetState.ClimbLeft  -> true
        else                   -> false
    }
}
