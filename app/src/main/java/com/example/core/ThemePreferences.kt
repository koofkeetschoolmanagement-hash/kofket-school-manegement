package com.example.core

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

private const val THEME_PREFS_NAME = "koofkeet_prefs"
private const val KEY_THEME_MODE = "selected_theme_mode"

fun loadSavedThemeMode(context: Context): AppThemeMode {
    val prefs = context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
    return AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.storageKey))
}

fun saveThemeMode(context: Context, mode: AppThemeMode) {
    context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_THEME_MODE, mode.storageKey)
        .apply()
}

@Composable
fun rememberAppThemeModeState(): Pair<AppThemeMode, (AppThemeMode) -> Unit> {
    val context = LocalContext.current
    var current by remember { mutableStateOf(loadSavedThemeMode(context)) }
    val update: (AppThemeMode) -> Unit = { newMode ->
        current = newMode
        saveThemeMode(context, newMode)
    }
    return current to update
}

@Composable
fun isDarkThemeActive(mode: AppThemeMode): Boolean = when (mode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
}
