package com.mikazuki.pocketfamiliar.model

data class FamiliarGift(
    val id: String,
    val displayName: String,
    val costCharms: Int,
    val interest: FamiliarInterest,
    val baseBondXp: Int,
)

object FamiliarGiftCatalog {
    val all: List<FamiliarGift> = listOf(
        FamiliarGift("snack", "Favorite Snack", 25, FamiliarInterest.FOOD, 18),
        FamiliarGift("toy_ball", "Tiny Ball", 35, FamiliarInterest.PLAY, 22),
        FamiliarGift("pillow", "Soft Pillow", 45, FamiliarInterest.SLEEP, 26),
        FamiliarGift("headphones", "Tiny Headphones", 60, FamiliarInterest.MUSIC, 32),
        FamiliarGift("book", "Pocket Book", 55, FamiliarInterest.READING, 30),
        FamiliarGift("walking_charm", "Walking Charm", 70, FamiliarInterest.WALKING, 38),
    )
}
