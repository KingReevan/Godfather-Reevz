package com.reevan.godfatherreevz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The app's fixed themes, each one complete look (light or dark). There is deliberately no
 * "follow system" option. Stored by name: never rename or remove an entry.
 */
enum class AppTheme(val label: String, val isDark: Boolean) {
    SICILIAN_SUNSET("Sicilian Sunset", isDark = false),
    SUNSET_AFTER_DARK("Sunset After Dark", isDark = true),
    CORLEONE_NOIR("Corleone Noir", isDark = true),
    LIMONCELLO("Limoncello", isDark = false),
    AMALFI("Amalfi Coast", isDark = false),
    NOTHING("Nothing", isDark = true),
}

/** App-wide preferences. Single row, id 1. New preferences belong here. */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val theme: AppTheme = AppTheme.SICILIAN_SUNSET,
    /** Call places the call straight away (needs CALL_PHONE); off opens the dialer instead. */
    val callDirectly: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
