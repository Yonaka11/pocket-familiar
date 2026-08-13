package com.mikazuki.pocketfamiliar.pet.behavior

/**
 * All possible states the pet can be in.
 *
 * Sealed class allows exhaustive when expressions and easy future extension
 * without breaking the existing state machine.
 */
sealed class PetState {
    /** Standing still, occasionally blinking. */
    data object Idle : PetState()

    /** Walking toward the left edge. */
    data object WalkLeft : PetState()

    /** Walking toward the right edge. */
    data object WalkRight : PetState()

    /** Dozing; entered from Idle after a long idle delay. */
    data object Sleep : PetState()

    /** User is actively holding and moving the pet. */
    data object Dragged : PetState()

    /** Falling under gravity after the user lets go. */
    data object Falling : PetState()

    // --- Reserved for future expansion ---
    // data object ClimbLeft  : PetState()
    // data object ClimbRight : PetState()
    // data object Hanging    : PetState()
    // data object Jumping    : PetState()
    // data object Sitting    : PetState()
    // data object Eating     : PetState()
    // data object Playing    : PetState()
    // data class  Custom(val id: String) : PetState()
}
