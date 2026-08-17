package com.mikazuki.pocketfamiliar.pet.behavior

/**
 * All possible familiar states. Movement/physics states are mixed with short
 * reaction states so character packs can provide personality-specific art.
 */
sealed class PetState {
    data object Idle : PetState()
    data object WalkLeft : PetState()
    data object WalkRight : PetState()
    data object RunLeft : PetState()
    data object RunRight : PetState()
    data object ClimbLeft : PetState()
    data object ClimbRight : PetState()
    data object Jumping : PetState()
    data object Sleep : PetState()
    data object DeepSleep : PetState()

    // Touch / physics reactions
    data object Held : PetState()
    data object Thrown : PetState()
    data object Falling : PetState()
    data object HardLanding : PetState()
    data object Recovering : PetState()

    // Cute autonomous reactions
    data object Eating : PetState()
    data object Grooming : PetState()
    data object Happy : PetState()

    // Context/activity reactions. Detection can force these states without
    // changing the animation model.
    data object Music : PetState()
    data object StepActivity : PetState()
    data object Charging : PetState()
    data object LowBattery : PetState()
}

val PetState.isWalking get() = this is PetState.WalkLeft || this is PetState.WalkRight
val PetState.isRunning get() = this is PetState.RunLeft || this is PetState.RunRight
val PetState.isClimbing get() = this is PetState.ClimbLeft || this is PetState.ClimbRight
val PetState.movesLeft get() = this is PetState.WalkLeft || this is PetState.RunLeft || this is PetState.ClimbLeft
val PetState.isAirborne get() = this is PetState.Jumping || this is PetState.Thrown || this is PetState.Falling
