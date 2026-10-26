package com.kevin.chatapplication.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Light / Dark / follow-the-phone theme, saved on this device. */
object ThemePrefs {

    private const val PREFS = "connectly_prefs"
    private const val KEY_THEME = "theme_mode"

    const val SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    const val LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val DARK = AppCompatDelegate.MODE_NIGHT_YES

    fun get(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_THEME, SYSTEM)

    fun set(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME, mode)
            .apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /** Call once when the app starts. */
    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(get(context))
    }
}
