package com.kevin.chatapplication.utils

import android.content.Context

/** Remembers whether the onboarding screens have been shown on this device. */
object OnboardingPrefs {

    private const val PREFS = "connectly_prefs"
    private const val KEY_SEEN = "onboarding_seen"

    fun isSeen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN, true)
            .apply()
    }
}
