package com.mikazuki.pocketfamiliar.data

import android.content.Context

/** Stores lightweight gameplay counters alongside the core familiar progress. */
class FamiliarGameplayRepository(context: Context) {
    private val store = context.getSharedPreferences("familiar_gameplay", Context.MODE_PRIVATE)

    fun giftCount(id: String): Int = counter(id, "gifts")

    fun addGift(id: String) {
        incrementCounter(id, "gifts")
    }

    fun counter(id: String, name: String): Int = store.getInt("${id}_$name", 0)

    fun incrementCounter(id: String, name: String, amount: Int = 1): Int {
        val next = counter(id, name) + amount.coerceAtLeast(0)
        store.edit().putInt("${id}_$name", next).apply()
        return next
    }

    fun achievementIds(id: String): Set<String> =
        store.getStringSet("${id}_achievements", emptySet())?.toSet() ?: emptySet()

    fun unlockAchievement(id: String, achievementId: String): Boolean {
        val current = achievementIds(id)
        if (achievementId in current) return false
        store.edit().putStringSet("${id}_achievements", current + achievementId).apply()
        return true
    }
}
