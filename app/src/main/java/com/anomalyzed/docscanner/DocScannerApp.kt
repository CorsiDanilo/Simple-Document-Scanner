package com.anomalyzed.docscanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DocScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
