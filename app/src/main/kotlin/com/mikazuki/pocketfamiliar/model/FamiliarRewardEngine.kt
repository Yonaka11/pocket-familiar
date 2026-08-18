package com.mikazuki.pocketfamiliar.model

object FamiliarRewardEngine {
    fun rewardForInterest(
        baseBondXp: Int,
        baseCharms: Int,
        interest: FamiliarInterest,
        preferences: FamiliarPreferences,
    ): FamiliarReward {
        val favorite = interest in preferences.favoriteInterests
        val multiplier = if (favorite) 1.25f else 1f
        return FamiliarReward(
            bondXp = (baseBondXp * multiplier).toInt().coerceAtLeast(baseBondXp),
            charms = baseCharms,
            preferenceBonusApplied = favorite,
        )
    }

    fun rewardForInteraction(
        interaction: TouchInteraction,
        preferences: FamiliarPreferences,
        combo: Int = 1,
    ): FamiliarReward {
        val favorite = interaction in preferences.favoriteTouch
        val basePlay = when (interaction) {
            TouchInteraction.TAP -> 1
            TouchInteraction.DOUBLE_TAP -> 2
            TouchInteraction.PET -> 2
            TouchInteraction.TICKLE -> 2
            TouchInteraction.THROW -> 1
            TouchInteraction.CATCH -> 3
            TouchInteraction.JUGGLE -> 4 + combo.coerceAtMost(10)
            TouchInteraction.SOFT_CATCH -> 5
            TouchInteraction.BOOP -> 2
            TouchInteraction.TRICK_THROW -> 8
            TouchInteraction.AIR_TIME -> 6
        }
        val multiplier = if (favorite) 1.25f else 1f
        return FamiliarReward(
            bondXp = 1,
            playXp = (basePlay * multiplier).toInt().coerceAtLeast(basePlay),
            preferenceBonusApplied = favorite,
        )
    }

    fun rewardForSteps(steps: Int, preferences: FamiliarPreferences): FamiliarReward {
        val chunks = steps.coerceAtLeast(0) / 250
        if (chunks == 0) return FamiliarReward()
        return rewardForInterest(chunks, chunks, FamiliarInterest.WALKING, preferences)
    }

    fun rewardForGift(gift: FamiliarGift, preferences: FamiliarPreferences): FamiliarReward =
        rewardForInterest(gift.baseBondXp, 0, gift.interest, preferences)
}
