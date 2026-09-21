package com.example.data

import android.content.Context

/**
 * Stores the user-supplied Gemini API key in SharedPreferences. The key is kept
 * on-device only; it is never shipped in the APK.
 */
class GeminiKeyStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)

    fun getKey(): String? =
        prefs.getString(KEY, null)?.takeIf { it.isNotBlank() }

    fun hasKey(): Boolean = !getKey().isNullOrBlank()

    fun saveKey(key: String) {
        prefs.edit().putString(KEY, key.trim()).apply()
    }

    fun clearKey() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "api_key"
    }
}