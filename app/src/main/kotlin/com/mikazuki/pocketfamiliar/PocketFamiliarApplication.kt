package com.mikazuki.pocketfamiliar

import android.app.Application
import android.content.Context

class PocketFamiliarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashReporter()
    }

    /**
     * Catches unhandled exceptions, writes a summary to SharedPreferences, then
     * re-throws to let the system handle the crash normally.
     * MainActivity reads this on the next launch and shows an AlertDialog so the
     * user can report the error without needing Logcat.
     */
    private fun installCrashReporter() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildString {
                    appendLine(throwable.javaClass.name + ": " + throwable.message)
                    throwable.stackTrace.take(10).forEach { frame ->
                        appendLine("  at ${frame.className}.${frame.methodName}" +
                                "(${frame.fileName}:${frame.lineNumber})")
                    }
                    throwable.cause?.let { cause ->
                        appendLine("Caused by: ${cause.javaClass.name}: ${cause.message}")
                        cause.stackTrace.take(5).forEach { frame ->
                            appendLine("  at ${frame.className}.${frame.methodName}" +
                                    "(${frame.fileName}:${frame.lineNumber})")
                        }
                    }
                }
                getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, report)
                    .commit()   // commit() is synchronous — must finish before process dies
            } catch (_: Exception) {
                // Never let the reporter itself crash
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val CRASH_PREFS = "pf_crash"
        const val KEY_LAST_CRASH = "last_crash"
    }
}
