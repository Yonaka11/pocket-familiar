package com.mikazuki.pocketfamiliar.pet.physics

/**
 * Character-specific physical feel. Values are intentionally game-like rather
 * than real-world units so each familiar can feel light, floaty, heavy, bouncy,
 * or stubborn without changing the core simulation.
 */
data class FamiliarPhysicsProfile(
    val mass: Float = 1.0f,
    val gravityScale: Float = 1.0f,
    val airDrag: Float = 0.65f,
    val restitution: Float = 0.34f,
    val floorFriction: Float = 0.72f,
    val maxThrowSpeed: Float = 2600f,
    val angularDamping: Float = 0.93f,
)

enum class ImpactSeverity {
    SOFT,
    NORMAL,
    HARD,
    CATASTROPHIC,
}
