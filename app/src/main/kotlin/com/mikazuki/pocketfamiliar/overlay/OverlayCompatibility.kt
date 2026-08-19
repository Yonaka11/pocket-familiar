package com.mikazuki.pocketfamiliar.overlay

import com.mikazuki.pocketfamiliar.model.FamiliarTheme

/**
 * Compatibility hooks for the overlay/theme split.
 * ThemeOverlayView now observes DataStore directly, so setTheme only needs to
 * invalidate immediately while persisted state propagates through its collector.
 */
fun ThemeOverlayView.setTheme(theme: FamiliarTheme) {
    @Suppress("UNUSED_VARIABLE")
    val ignored = theme
    invalidate()
}

/**
 * Keeps the service's delta-based tick call source-compatible while the theme
 * layer drives its own animation with postInvalidateOnAnimation().
 */
fun PetOverlayManager.tick(deltaSeconds: Float) {
    @Suppress("UNUSED_VARIABLE")
    val ignored = deltaSeconds
    tick()
}
