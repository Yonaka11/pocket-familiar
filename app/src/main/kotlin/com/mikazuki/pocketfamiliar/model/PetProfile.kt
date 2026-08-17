package com.mikazuki.pocketfamiliar.model

import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import com.mikazuki.pocketfamiliar.pet.physics.FamiliarPhysicsProfile

/** Defines one selectable familiar, its animations, and its physical feel. */
data class PetProfile(
    val id: String,
    val displayName: String,
    val description: String,
    val previewResId: Int,
    val idleAnim: PetAnimation,
    val walkAnim: PetAnimation,
    val runAnim: PetAnimation,
    val sleepAnim: PetAnimation,
    val fallAnim: PetAnimation,
    val climbAnim: PetAnimation,
    val jumpAnim: PetAnimation,
    val holdAnim: PetAnimation = fallAnim,
    val throwAnim: PetAnimation = jumpAnim,
    val physics: FamiliarPhysicsProfile = FamiliarPhysicsProfile(),
) {
    fun animationForState(state: PetState): PetAnimation = when (state) {
        is PetState.Idle -> idleAnim
        is PetState.WalkLeft,
        is PetState.WalkRight -> walkAnim
        is PetState.RunLeft,
        is PetState.RunRight -> runAnim
        is PetState.Sleep -> sleepAnim
        is PetState.Falling -> fallAnim
        is PetState.Held -> holdAnim
        is PetState.Thrown -> throwAnim
        is PetState.ClimbLeft,
        is PetState.ClimbRight -> climbAnim
        is PetState.Jumping -> jumpAnim
    }

    fun isFlippedForState(state: PetState): Boolean = when (state) {
        is PetState.WalkLeft,
        is PetState.RunLeft,
        is PetState.ClimbLeft -> true
        else -> false
    }
}
