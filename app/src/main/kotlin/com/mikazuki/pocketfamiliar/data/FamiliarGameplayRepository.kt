package com.mikazuki.pocketfamiliar.data

import android.content.Context

/** Stores lightweight gameplay counters alongside the core familiar progress. */
class FamiliarGameplayRepository(context: Context) {
    private val store = context.getSharedPreferences("familiar_gameplay", Context.MODE_PRIVATE)

    fun giftCount(id: String): Int = store.getInt("${id}_gifts", 0)

    fun addGift(id: String) {
        store.edit().putInt("${id}_gifts", giftCount(id) + 1).apply()
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
