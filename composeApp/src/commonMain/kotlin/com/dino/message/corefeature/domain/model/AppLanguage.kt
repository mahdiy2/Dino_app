package com.dino.message.corefeature.domain.model

/**
 * The `AppLanguage` enum class represents the different languages available in the application.
 */
enum class AppLanguage {
    /**
     * Represents the English language.
     */
    English,

    /**
     * Represents the Farsi language.
     */
    Farsi
}

/**
 * Converts the `AppLanguage` enum value to its corresponding locale string.
 *
 * @return The locale string representation of the language.
 */
fun AppLanguage.toLocale(): String {
    return when (this) {
        AppLanguage.English -> "en"
        AppLanguage.Farsi -> "fa"
    }
}

/**
 * Converts a locale string to its corresponding `AppLanguage` enum value.
 *
 * @return The `AppLanguage` enum value corresponding to the locale string.
 */
fun String.toAppLanguage(): AppLanguage {
    return when (this) {
        "fa" -> AppLanguage.Farsi
        else -> AppLanguage.English
    }
}
