package com.mikazuki.pocketfamiliar.service

import com.mikazuki.pocketfamiliar.overlay.PetOverlayManager

/**
 * Source-compatibility bridge while the service still passes frame delta.
 * Theme animation is self-driven, so the manager only needs its normal pet tick.
 */
fun PetOverlayManager.tick(deltaSeconds: Float) {
    @Suppress("UNUSED_VARIABLE")
    val ignored = deltaSeconds
    tick()
}
