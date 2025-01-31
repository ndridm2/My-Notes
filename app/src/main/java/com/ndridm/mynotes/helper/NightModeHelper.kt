package com.ndridm.mynotes.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object NightModeHelper {
    private const val PREF_NAME = "theme_pref"
    private const val THEME_KEY = "theme_mode"

    fun saveThemeMode(context: Context, isNighMode: Boolean) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(THEME_KEY, isNighMode).apply()
    }

    fun loadThemeMode(context: Context) : Boolean {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(THEME_KEY, false) // default light mode
    }

    fun applyTheme(context: Context) {
        val isNightMode = loadThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}