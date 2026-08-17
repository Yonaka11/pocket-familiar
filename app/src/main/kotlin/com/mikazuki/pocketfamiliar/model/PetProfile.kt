package com.mikazuki.pocketfamiliar.model

import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import com.mikazuki.pocketfamiliar.pet.physics.FamiliarPhysicsProfile

/** Defines one selectable familiar, its animations, preferences, and physical feel. */
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
    val hardLandAnim: PetAnimation = fallAnim,
    val recoverAnim: PetAnimation = idleAnim,
    val eatAnim: PetAnimation = idleAnim,
    val groomAnim: PetAnimation = idleAnim,
    val happyAnim: PetAnimation = idleAnim,
    val musicAnim: PetAnimation = idleAnim,
    val stepAnim: PetAnimation = runAnim,
    val chargingAnim: PetAnimation = sleepAnim,
    val lowBatteryAnim: PetAnimation = sleepAnim,
    val deepSleepAnim: PetAnimation = sleepAnim,
    val preferences: FamiliarPreferences = FamiliarPreferences(),
    val physics: FamiliarPhysicsProfile = FamiliarPhysicsProfile(),
) {
    fun animationForState(state: PetState): PetAnimation = when (state) {
        is PetState.Idle -> idleAnim
        is PetState.WalkLeft,
        is PetState.WalkRight -> walkAnim
        is PetState.RunLeft,
        is PetState.RunRight -> runAnim
        is PetState.Sleep -> sleepAnim
        is PetState.DeepSleep -> deepSleepAnim
        is PetState.Falling -> fallAnim
        is PetState.Held -> holdAnim
        is PetState.Thrown -> throwAnim
        is PetState.HardLanding -> hardLandAnim
        is PetState.Recovering -> recoverAnim
        is PetState.Eating -> eatAnim
        is PetState.Grooming -> groomAnim
        is PetState.Happy -> happyAnim
        is PetState.Music -> musicAnim
        is PetState.StepActivity -> stepAnim
        is PetState.Charging -> chargingAnim
        is PetState.LowBattery -> lowBatteryAnim
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
