package com.mikazuki.pocketfamiliar.pet.behavior

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "PetStateMachine"

class PetStateMachine(
    private val scope: CoroutineScope,
    private val onStateChanged: (PetState) -> Unit,
    private val isSleepEnabled: () -> Boolean,
) {
    var currentState: PetState = PetState.Idle
        private set

    private var transitionJob: Job? = null

    fun start() { transitionTo(PetState.Idle) }

    fun stop() {
        transitionJob?.cancel()
        transitionJob = null
    }

    fun forceState(newState: PetState) { transitionTo(newState) }

    private fun transitionTo(newState: PetState) {
        transitionJob?.cancel()
        currentState = newState
        onStateChanged(newState)
        Log.d(TAG, "→ ${newState::class.simpleName}")

        transitionJob = when (newState) {
            is PetState.Idle -> scheduleFromIdle()
            is PetState.WalkLeft,
            is PetState.WalkRight -> scheduleFromWalk()
            is PetState.RunLeft,
            is PetState.RunRight -> scheduleFromRun()
            is PetState.Sleep -> scheduleFromSleep()
            is PetState.ClimbLeft,
            is PetState.ClimbRight -> scheduleFromClimb()
            is PetState.Held,
            is PetState.Thrown,
            is PetState.Falling,
            is PetState.Jumping -> null
        }
    }

    private fun scheduleFromIdle(): Job = scope.launch {
        delay(Random.nextLong(1_500, 4_000))
        val r = Random.nextFloat()
        val next = when {
            isSleepEnabled() && r < 0.15f -> PetState.Sleep
            r < 0.45f -> PetState.WalkLeft
            r < 0.70f -> PetState.WalkRight
            r < 0.85f -> PetState.RunLeft
            else -> PetState.RunRight
        }
        transitionTo(next)
    }

    private fun scheduleFromWalk(): Job = scope.launch {
        delay(Random.nextLong(2_000, 5_000))
        transitionTo(PetState.Idle)
    }

    private fun scheduleFromRun(): Job = scope.launch {
        delay(Random.nextLong(1_200, 3_500))
        transitionTo(PetState.Idle)
    }

    private fun scheduleFromSleep(): Job = scope.launch {
        delay(Random.nextLong(4_000, 10_000))
        transitionTo(PetState.Idle)
    }

    private fun scheduleFromClimb(): Job = scope.launch {
        delay(Random.nextLong(800, 2_500))
        transitionTo(PetState.Jumping)
    }
}
