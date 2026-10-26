package com.kevin.chatapplication

import android.app.Application
import com.kevin.chatapplication.utils.ThemePrefs

class ConnectlyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply the saved Light / Dark / System choice before any screen opens.
        ThemePrefs.apply(this)
    }
}
