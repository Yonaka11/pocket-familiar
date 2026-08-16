package com.mikazuki.pocketfamiliar.pet.behavior

/**
 * All possible states the pet can be in.
 *
 * Sealed class enables exhaustive when-expressions and safe future extension.
 */
sealed class PetState {
    /** Standing still. */
    data object Idle : PetState()

    /** Walking at normal speed toward the left edge. */
    data object WalkLeft : PetState()

    /** Walking at normal speed toward the right edge. */
    data object WalkRight : PetState()

    /** Running (faster walk) toward the left edge. */
    data object RunLeft : PetState()

    /** Running (faster walk) toward the right edge. */
    data object RunRight : PetState()

    /** Ascending along the left screen edge. */
    data object ClimbLeft : PetState()

    /** Ascending along the right screen edge. */
    data object ClimbRight : PetState()

    /**
     * Leaping off a wall edge into the screen.
     * Physics engine applies a horizontal impulse and gravity until landing.
     */
    data object Jumping : PetState()

    /** Dozing; entered from Idle after a long idle delay. */
    data object Sleep : PetState()

    /** User is actively holding and moving the pet. */
    data object Dragged : PetState()

    /** Falling under gravity after being released or jumping off a wall. */
    data object Falling : PetState()
}

/** Convenience groups used by physics and the state machine. */
val PetState.isWalking get() = this is PetState.WalkLeft || this is PetState.WalkRight
val PetState.isRunning get() = this is PetState.RunLeft  || this is PetState.RunRight
val PetState.isClimbing get() = this is PetState.ClimbLeft || this is PetState.ClimbRight
val PetState.movesLeft get() = this is PetState.WalkLeft || this is PetState.RunLeft  || this is PetState.ClimbLeft
