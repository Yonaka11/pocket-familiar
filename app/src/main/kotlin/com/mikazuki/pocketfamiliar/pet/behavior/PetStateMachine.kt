package com.mikazuki.pocketfamiliar.pet.behavior

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "PetStateMachine"

/**
 * Drives autonomous state transitions for the pet.
 *
 * The machine runs scheduled transitions on the provided [scope]. Calling
 * [forceState] from outside (e.g. dragging) cancels any pending transition
 * and immediately applies the new state.
 */
class PetStateMachine(
    private val scope: CoroutineScope,
    private val onStateChanged: (PetState) -> Unit,
    private val isSleepEnabled: () -> Boolean,
) {
    var currentState: PetState = PetState.Idle
        private set

    private var transitionJob: Job? = null

    /** Start the autonomous behavior loop from the Idle state. */
    fun start() {
        transitionTo(PetState.Idle)
    }

    /** Stop all scheduled transitions (call on service stop). */
    fun stop() {
        transitionJob?.cancel()
        transitionJob = null
    }

    /**
     * Override the state externally (drag start/end, physics landing).
     * Resumes autonomous scheduling from the new state where appropriate.
     */
    fun forceState(newState: PetState) {
        transitionTo(newState)
    }

    private fun transitionTo(newState: PetState) {
        transitionJob?.cancel()
        currentState = newState
        onStateChanged(newState)
        Log.d(TAG, "→ $newState")

        transitionJob = when (newState) {
            is PetState.Idle -> scheduleIdleTransition()
            is PetState.WalkLeft -> scheduleWalkTransition()
            is PetState.WalkRight -> scheduleWalkTransition()
            is PetState.Sleep -> scheduleSleepTransition()
            // External events (drag/fall/land) control these states; no timer needed.
            is PetState.Dragged, is PetState.Falling -> null
        }
    }

    private fun scheduleIdleTransition(): Job = scope.launch {
        val idleMs = Random.nextLong(1_500, 4_000)
        delay(idleMs)

        val next = when {
            isSleepEnabled() && Random.nextFloat() < 0.20f -> PetState.Sleep
            Random.nextBoolean() -> PetState.WalkLeft
            else -> PetState.WalkRight
        }
        transitionTo(next)
    }

    private fun scheduleWalkTransition(): Job = scope.launch {
        val walkMs = Random.nextLong(2_000, 5_000)
        delay(walkMs)
        transitionTo(PetState.Idle)
    }

    private fun scheduleSleepTransition(): Job = scope.launch {
        val sleepMs = Random.nextLong(4_000, 10_000)
        delay(sleepMs)
        transitionTo(PetState.Idle)
    }
}
