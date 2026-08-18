package com.mikazuki.pocketfamiliar.model

/** Canon-level identity data kept separate from drawable/animation assets. */
enum class FamiliarFormKind {
    DAY_HUMAN,
    NIGHT_SPIRIT_BASE,
    NIGHT_SPIRIT_ASCENDED,
    HUMAN_RESTORED,
}

data class FamiliarForm(
    val id: String,
    val displayName: String,
    val kind: FamiliarFormKind,
    val summary: String,
)

data class FamiliarCanonProfile(
    val id: String,
    val displayName: String,
    val role: String,
    val forms: List<FamiliarForm>,
)

data class BossRestorationRequirement(
    val requiredFamiliarIds: Set<String>,
    val minimumBondLevelEach: Int,
    val minimumFamiliarLevelEach: Int,
    val requiredSignatureMemoriesEach: Int,
    val requiresDayNightAchievementChain: Boolean = true,
)

/**
 * Locked Pocket Familiar story canon.
 *
 * EMK are Seraphi Astrea's attendants. During the day they can manifest their
 * humanoid forms; at night their spirit-animal forms dominate. Seraphi is
 * trapped in spirit form and the long-term player goal is restoring her human
 * form by strengthening all three attendants and recovering shared memories.
 */
object PocketFamiliarCanon {
    val emi = FamiliarCanonProfile(
        id = "emi",
        displayName = "Emi",
        role = "Sparkborn Trickster; Seraphi's attendant of play and rhythm",
        forms = listOf(
            FamiliarForm("emi_day", "Tech-Royal Familiar", FamiliarFormKind.DAY_HUMAN, "Dark-skinned, bold, energetic and touch-play focused."),
            FamiliarForm("emi_night", "Sparkborn Trickster", FamiliarFormKind.NIGHT_SPIRIT_BASE, "A fast dark spirit beast with amber eyes, electric cobalt-gold trails and playful trickster energy."),
            FamiliarForm("emi_night_ascended", "Sparkborn Ascended", FamiliarFormKind.NIGHT_SPIRIT_ASCENDED, "A brighter, faster spirit form with amplified circuit, lightning and rhythm motifs."),
        ),
    )

    val kaelani = FamiliarCanonProfile(
        id = "kaelani",
        displayName = "Kaelani",
        role = "Bloom Spirit; Seraphi's attendant of warmth and harmony",
        forms = listOf(
            FamiliarForm("kaelani_day", "Graceful Bloom Familiar", FamiliarFormKind.DAY_HUMAN, "Warm, elegant and grounded with teal, bronze and earthy-gold styling."),
            FamiliarForm("kaelani_night", "Bloom Spirit", FamiliarFormKind.NIGHT_SPIRIT_BASE, "An elegant floral spirit beast with moonlit petals, warm gold and teal accents."),
            FamiliarForm("kaelani_night_ascended", "Bloom Spirit Ascended", FamiliarFormKind.NIGHT_SPIRIT_ASCENDED, "A radiant moon-bloom form with richer petal trails, blessing effects and graceful celestial ornament."),
        ),
    )

    val mira = FamiliarCanonProfile(
        id = "mira",
        displayName = "Mira",
        role = "Dreamwatch Scholar; Seraphi's attendant of memory and dreams",
        forms = listOf(
            FamiliarForm("mira_day", "Cozy Scholar Familiar", FamiliarFormKind.DAY_HUMAN, "Thoughtful, sleepy and bookish with muted teal, charcoal, burgundy and brass styling."),
            FamiliarForm("mira_night", "Dreamwatch Scholar", FamiliarFormKind.NIGHT_SPIRIT_BASE, "A soft moonlit scholar beast with glasses, dream notes, lantern and book motifs."),
            FamiliarForm("mira_night_ascended", "Dreamwatch Ascended", FamiliarFormKind.NIGHT_SPIRIT_ASCENDED, "A more luminous dream-keeper form with stronger star, archive and memory effects."),
        ),
    )

    val seraphiAstrea = FamiliarCanonProfile(
        id = "seraphi_astrea",
        displayName = "Seraphi Astrea",
        role = "Boss; Seraphim Celestial Keeper",
        forms = listOf(
            FamiliarForm("seraphi_spirit", "Celestial Seraphim Familiar", FamiliarFormKind.NIGHT_SPIRIT_BASE, "A compact cream-and-gold seraphic spirit beast with six wings and a constantly orbiting halo."),
            FamiliarForm("seraphi_spirit_ascended", "Ascended Seraphim", FamiliarFormKind.NIGHT_SPIRIT_ASCENDED, "Her level-up spirit form: larger, more radiant, six wings fully expressed, stronger sacred markings and a more complex halo system."),
            FamiliarForm("seraphi_human_restored", "Seraphim Celestial Keeper", FamiliarFormKind.HUMAN_RESTORED, "Seraphi's true restored human form, unlocked through EMK progression, achievements and recovered memories."),
        ),
    )

    val coreCast = listOf(emi, kaelani, mira, seraphiAstrea)

    val seraphiHumanRestoration = BossRestorationRequirement(
        requiredFamiliarIds = setOf(emi.id, kaelani.id, mira.id),
        minimumBondLevelEach = 10,
        minimumFamiliarLevelEach = 15,
        requiredSignatureMemoriesEach = 3,
    )
}
