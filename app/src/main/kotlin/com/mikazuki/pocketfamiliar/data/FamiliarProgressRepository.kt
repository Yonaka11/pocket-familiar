package com.mikazuki.pocketfamiliar.data

import android.content.Context
import com.mikazuki.pocketfamiliar.model.FamiliarProgress
import com.mikazuki.pocketfamiliar.model.FamiliarReward

class FamiliarProgressRepository(context: Context) {
    private val store = context.getSharedPreferences("familiar_progress", Context.MODE_PRIVATE)

    fun load(id: String): FamiliarProgress = FamiliarProgress(
        familiarId = id,
        bondXp = store.getInt("${id}_bond", 0),
        playXp = store.getInt("${id}_play", 0),
        charms = store.getInt("${id}_charms", 0),
        lifetimeSteps = store.getInt("${id}_steps", 0),
        bestJuggleCombo = store.getInt("${id}_juggle", 0),
    )

    fun addReward(id: String, reward: FamiliarReward): FamiliarProgress {
        val current = load(id)
        val next = current.copy(
            bondXp = current.bondXp + reward.bondXp,
            playXp = current.playXp + reward.playXp,
            charms = current.charms + reward.charms,
        )
        save(next)
        return next
    }

    fun addSteps(id: String, amount: Int): FamiliarProgress {
        val current = load(id)
        val next = current.copy(lifetimeSteps = current.lifetimeSteps + amount.coerceAtLeast(0))
        save(next)
        return next
    }

    fun recordJuggle(id: String, combo: Int): FamiliarProgress {
        val current = load(id)
        if (combo <= current.bestJuggleCombo) return current
        val next = current.copy(bestJuggleCombo = combo)
        save(next)
        return next
    }

    private fun save(value: FamiliarProgress) {
        store.edit()
            .putInt("${value.familiarId}_bond", value.bondXp)
            .putInt("${value.familiarId}_play", value.playXp)
            .putInt("${value.familiarId}_charms", value.charms)
            .putInt("${value.familiarId}_steps", value.lifetimeSteps)
            .putInt("${value.familiarId}_juggle", value.bestJuggleCombo)
            .apply()
    }
}
