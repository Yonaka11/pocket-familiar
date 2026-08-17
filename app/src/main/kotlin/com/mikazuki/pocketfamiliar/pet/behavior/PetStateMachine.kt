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
            is PetState.DeepSleep -> scheduleReactionReturn(6_000L..12_000L)
            is PetState.ClimbLeft,
            is PetState.ClimbRight -> scheduleFromClimb()
            is PetState.Eating -> scheduleReactionReturn(1_800L..3_200L)
            is PetState.Grooming -> scheduleReactionReturn(1_800L..3_000L)
            is PetState.Happy -> scheduleReactionReturn(1_200L..2_400L)
            is PetState.StepActivity -> scheduleReactionReturn(1_600L..2_800L)
            is PetState.HardLanding -> scheduleRecover()
            is PetState.Recovering -> scheduleReactionReturn(700L..1_400L)
            is PetState.Music,
            is PetState.Charging,
            is PetState.LowBattery,
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
            isSleepEnabled() && r < 0.10f -> PetState.Sleep
            r < 0.17f -> PetState.Eating
            r < 0.24f -> PetState.Grooming
            r < 0.30f -> PetState.Happy
            r < 0.52f -> PetState.WalkLeft
            r < 0.74f -> PetState.WalkRight
            r < 0.87f -> PetState.RunLeft
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
        delay(Random.nextLong(4_000, 9_000))
        transitionTo(if (isSleepEnabled() && Random.nextFloat() < 0.35f) PetState.DeepSleep else PetState.Idle)
    }

    private fun scheduleFromClimb(): Job = scope.launch {
        delay(Random.nextLong(800, 2_500))
        transitionTo(PetState.Jumping)
    }

    private fun scheduleReactionReturn(duration: LongRange): Job = scope.launch {
        delay(Random.nextLong(duration.first, duration.last + 1))
        transitionTo(PetState.Idle)
    }

    private fun scheduleRecover(): Job = scope.launch {
        delay(Random.nextLong(500, 1_000))
        transitionTo(PetState.Recovering)
    }
}
