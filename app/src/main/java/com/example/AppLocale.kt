package com.example

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * In-app language selection. The app ignores the device locale and always uses
 * the code stored in preferences ("en" or "my"), applied via a configuration
 * context so string resources resolve to the matching values/values-my folder.
 */
object AppLocale {
    const val PREF_FILE = "widget_prefs"
    const val KEY_LANGUAGE = "app_language"
    const val DEFAULT = "en"

    fun current(context: Context): String =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT)
            ?: DEFAULT

    fun isBurmese(context: Context): Boolean = current(context) == "my"

    fun apply(context: Context): Context {
        return context.createConfigurationContext(configurationWithLocale(context))
    }

    /** New base configuration (overrides previous locale recursively on API 24+). */
    fun configurationWithLocale(context: Context): Configuration {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(current(context)))
        return configuration
    }
}