package com.mikazuki.pocketfamiliar.model

enum class FamiliarFormKind {
    DAY_HUMANOID,
    NIGHT_SPIRIT,
    ASCENDED_SPIRIT,
    RESTORED_HUMAN,
}

data class FamiliarFormDefinition(
    val id: String,
    val displayName: String,
    val kind: FamiliarFormKind,
    val title: String,
    val summary: String,
    val visualHooks: List<String>,
)

data class CanonFamiliar(
    val id: String,
    val displayName: String,
    val role: String,
    val personalityCore: List<String>,
    val forms: List<FamiliarFormDefinition>,
)

/**
 * Canon-facing roster. This intentionally lives beside, rather than inside,
 * PetRegistry so story/lore can evolve without forcing unfinished art into the
 * currently selectable runtime character list.
 */
object PocketFamiliarCanon {
    val seraphiAstrea = CanonFamiliar(
        id = "seraphi_astrea",
        displayName = "Seraphi Astrea",
        role = "Boss / Celestial Keeper / Seraphim",
        personalityCore = listOf("ancient", "kind", "regal", "protective", "luminous"),
        forms = listOf(
            FamiliarFormDefinition(
                id = "seraphi_spirit_base",
                displayName = "Seraphi Astrea",
                kind = FamiliarFormKind.NIGHT_SPIRIT,
                title = "Celestial Seraphim Familiar",
                summary = "Her sealed base form: a small luminous spirit beast with six restrained wings and a constantly orbiting halo.",
                visualHooks = listOf("cream-white coat", "golden eyes", "six-wing motif", "orbiting halo rings", "celestial sigils"),
            ),
            FamiliarFormDefinition(
                id = "seraphi_spirit_ascended",
                displayName = "Seraphi Astrea",
                kind = FamiliarFormKind.ASCENDED_SPIRIT,
                title = "Ascended Seraphim",
                summary = "Her level-up spirit form. The six wings fully manifest, halo geometry grows more complex, and sacred markings become radiant.",
                visualHooks = listOf("six fully expressed wings", "multi-ring halo", "radiant markings", "larger celestial silhouette"),
            ),
            FamiliarFormDefinition(
                id = "seraphi_human_restored",
                displayName = "Seraphi Astrea",
                kind = FamiliarFormKind.RESTORED_HUMAN,
                title = "Seraphim Celestial Keeper",
                summary = "Her true human form, restored only after Emi, Kaelani, and Mira recover enough bond, memories, and achievements.",
                visualHooks = listOf("six angelic wings", "ivory-and-gold regalia", "celestial staff", "halo crown", "golden eyes"),
            ),
        ),
    )

    val emi = CanonFamiliar(
        id = "emi",
        displayName = "Emi",
        role = "Playful Tech-Royal Attendant",
        personalityCore = listOf("playful", "bold", "spirited", "teasing", "attention-loving"),
        forms = listOf(
            FamiliarFormDefinition(
                id = "emi_day",
                displayName = "Emi",
                kind = FamiliarFormKind.DAY_HUMANOID,
                title = "Playful Tech-Royal",
                summary = "Her daytime humanoid familiar form, built around music, play, touch interactions, and kinetic movement.",
                visualHooks = listOf("very dark skin", "amber-gold eyes", "black bob with yellow accents", "black/yellow/cobalt palette"),
            ),
            FamiliarFormDefinition(
                id = "emi_night",
                displayName = "Emi",
                kind = FamiliarFormKind.NIGHT_SPIRIT,
                title = "Sparkborn Trickster",
                summary = "A fast cat-fox-rabbit-like electric spirit that turns nighttime into a game.",
                visualHooks = listOf("shadow-dark coat", "amber eyes", "electric cobalt streaks", "gold crest", "fast luminous halo trail"),
            ),
        ),
    )

    val kaelani = CanonFamiliar(
        id = "kaelani",
        displayName = "Kaelani",
        role = "Graceful Bloom Attendant",
        personalityCore = listOf("graceful", "warm", "grounded", "affectionate", "composed"),
        forms = listOf(
            FamiliarFormDefinition(
                id = "kaelani_day",
                displayName = "Kaelani",
                kind = FamiliarFormKind.DAY_HUMANOID,
                title = "Graceful Bloom",
                summary = "Her daytime humanoid form: warm, elegant, musical, and gift-oriented.",
                visualHooks = listOf("warm golden-brown skin", "hazel-gold eyes", "flowing dark curls", "teal/bronze/earthy-gold palette"),
            ),
            FamiliarFormDefinition(
                id = "kaelani_night",
                displayName = "Kaelani",
                kind = FamiliarFormKind.NIGHT_SPIRIT,
                title = "Bloom Spirit",
                summary = "An elegant floral moon spirit whose movement leaves petals, ribbons of teal light, and soft golden blooms.",
                visualHooks = listOf("deer-cat-fox silhouette", "floral antler/ear accents", "teal ribbons", "golden petals", "moonlit bloom halo"),
            ),
        ),
    )

    val mira = CanonFamiliar(
        id = "mira",
        displayName = "Mira",
        role = "Cozy Scholar Attendant",
        personalityCore = listOf("thoughtful", "shy", "sleepy", "sweet", "comfort-seeking"),
        forms = listOf(
            FamiliarFormDefinition(
                id = "mira_day",
                displayName = "Mira",
                kind = FamiliarFormKind.DAY_HUMANOID,
                title = "Cozy Scholar",
                summary = "Her daytime humanoid form, happiest around books, snacks, gifts, naps, and gentle attention.",
                visualHooks = listOf("petite scholarly silhouette", "glasses", "muted teal/charcoal/burgundy/brass palette", "cozy accessories"),
            ),
            FamiliarFormDefinition(
                id = "mira_night",
                displayName = "Mira",
                kind = FamiliarFormKind.NIGHT_SPIRIT,
                title = "Dreamwatch Scholar",
                summary = "A rounded nocturnal dream spirit that reads glowing books, guards memories, and curls up beneath a moonlike halo.",
                visualHooks = listOf("cat-owl-rabbit silhouette", "round glasses", "moon halo", "lantern and book motifs", "soft gray-teal fur"),
            ),
        ),
    )

    val attendants = listOf(emi, kaelani, mira)
    val all = listOf(seraphiAstrea) + attendants
}
