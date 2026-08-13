package com.mikazuki.pocketfamiliar

import android.app.Application

class PocketFamiliarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Global application setup goes here when needed.
        // Kept minimal for the MVP; no DI framework required.
    }
}
