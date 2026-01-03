package com.atuy.prefix_dialer

import android.app.Application
import com.google.android.material.color.DynamicColors

class PrefixDialerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color palettes on supported devices to keep UI aligned with system theme
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
