package com.mikazuki.pocketfamiliar.pet.behavior

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "PetStateMachine"

/**
 * Drives all autonomous state transitions for the pet.
 *
 * The machine schedules future transitions on the provided [scope]. External
 * code (touch events, physics boundary hits) calls [forceState] to override
 * the schedule immediately.
 *
 * Transition graph (simplified):
 *
 *   Idle → WalkLeft | WalkRight | RunLeft | RunRight | Sleep
 *   Walk/Run → Idle (after random duration)
 *   Idle/Walk/Run → ClimbLeft | ClimbRight (when forced by physics on reaching edge)
 *   Climb → Jumping (after climbing for a bit)
 *   Jumping/Falling → Idle (on physics landing)
 *   Any → Dragged (user touch)
 *   Dragged → Falling (user release)
 */
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
            is PetState.Idle       -> scheduleFromIdle()
            is PetState.WalkLeft,
            is PetState.WalkRight  -> scheduleFromWalk()
            is PetState.RunLeft,
            is PetState.RunRight   -> scheduleFromRun()
            is PetState.Sleep      -> scheduleFromSleep()
            is PetState.ClimbLeft,
            is PetState.ClimbRight -> scheduleFromClimb(newState)
            // Physics / user events control these; no timer needed.
            is PetState.Dragged,
            is PetState.Falling,
            is PetState.Jumping    -> null
        }
    }

    // ── Scheduled transitions ────────────────────────────────────────────────

    private fun scheduleFromIdle(): Job = scope.launch {
        delay(Random.nextLong(1_500, 4_000))
        val r = Random.nextFloat()
        val next = when {
            isSleepEnabled() && r < 0.15f -> PetState.Sleep
            r < 0.45f -> PetState.WalkLeft
            r < 0.70f -> PetState.WalkRight
            r < 0.85f -> PetState.RunLeft
            else       -> PetState.RunRight
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

    /**
     * Climbing up an edge: after a random height the pet launches off into the
     * screen ([PetState.Jumping]).  Physics handles the arc; it lands and calls
     * [forceState](PetState.Idle) via [ForcedTransition.Land].
     */
    private fun scheduleFromClimb(climbState: PetState): Job = scope.launch {
        val climbDurationMs = Random.nextLong(800, 2_500)
        delay(climbDurationMs)
        transitionTo(PetState.Jumping)
    }
}
