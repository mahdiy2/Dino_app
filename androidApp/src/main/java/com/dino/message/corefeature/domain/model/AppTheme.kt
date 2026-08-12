package com.dino.message.corefeature.domain.model

/**
 * The `AppTheme` enum class represents the different themes available in the application.
 */
enum class AppTheme {
    /**
     * Represents the dark theme.
     */
    DarkTheme,

    /**
     * Represents the light theme.
     */
    LightTheme,

    /**
     * Represents the theme based on system settings.
     */
    SystemSettings
}

/**
 * Converts the `AppTheme` enum value to a boolean representing the theme selection.
 *
 * @param systemSettings A flag that shows which theme is selected in system settings.
 * @return The boolean value representing the theme selection.
 */
fun AppTheme.toBoolean(systemSettings: Boolean) = when (this) {
    AppTheme.SystemSettings -> systemSettings
    AppTheme.LightTheme -> false
    AppTheme.DarkTheme -> true
}
