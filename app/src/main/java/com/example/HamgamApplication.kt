package com.example

import android.app.Application
import com.example.feedback.CrashLogStore

/**
 * App entry point. Its one job today is installing the crash-report writer
 * before anything else can crash, so a technical report is kept for the
 * feedback form (see CrashLogStore).
 */
class HamgamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogStore.install(this)
    }
}
