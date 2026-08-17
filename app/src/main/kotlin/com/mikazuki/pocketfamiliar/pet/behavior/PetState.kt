package com.mikazuki.pocketfamiliar.pet.behavior

/** All possible states the pet can be in. */
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
    data object Held : PetState()
    data object Thrown : PetState()
    data object Falling : PetState()
}

val PetState.isWalking get() = this is PetState.WalkLeft || this is PetState.WalkRight
val PetState.isRunning get() = this is PetState.RunLeft || this is PetState.RunRight
val PetState.isClimbing get() = this is PetState.ClimbLeft || this is PetState.ClimbRight
val PetState.movesLeft get() = this is PetState.WalkLeft || this is PetState.RunLeft || this is PetState.ClimbLeft
