package com.example.widget

import android.content.Context
import androidx.annotation.ColorInt
import com.example.R

/**
 * Global widget theme presets. Each palette maps to a background drawable and
 * text colors applied via RemoteViews in [VocabWidgetProvider].
 */
data class WidgetTheme(
    val key: String,
    val backgroundRes: Int,
    @ColorInt val background: Int,
    @ColorInt val word: Int,
    @ColorInt val reading: Int,
    @ColorInt val meaning: Int,
    @ColorInt val type: Int,
    @ColorInt val chapter: Int,
    @ColorInt val notes: Int,
    @ColorInt val example: Int,
    @ColorInt val backHeader: Int
)

object WidgetThemes {

    const val PREF_THEME = "widget_theme"
    const val DEFAULT_KEY = "dark"

    val all: List<WidgetTheme> = listOf(
        WidgetTheme(
            key = "dark",
            backgroundRes = R.drawable.widget_background,
            background = 0xFF1A1F26.toInt(),
            word = 0xFFFFFFFF.toInt(),
            reading = 0xFF60A5FA.toInt(),
            meaning = 0xFFD1D5DB.toInt(),
            type = 0xFF8A94A6.toInt(),
            chapter = 0xFF6A7280.toInt(),
            notes = 0xFFFFFFFF.toInt(),
            example = 0xFF34D399.toInt(),
            backHeader = 0xFF60A5FA.toInt()
        ),
        WidgetTheme(
            key = "light",
            backgroundRes = R.drawable.widget_background_light,
            background = 0xFFFFFFFF.toInt(),
            word = 0xFF0F172A.toInt(),
            reading = 0xFF2563EB.toInt(),
            meaning = 0xFF475569.toInt(),
            type = 0xFF64748B.toInt(),
            chapter = 0xFF94A3B8.toInt(),
            notes = 0xFF0F172A.toInt(),
            example = 0xFF059669.toInt(),
            backHeader = 0xFF2563EB.toInt()
        ),
        WidgetTheme(
            key = "midnight",
            backgroundRes = R.drawable.widget_background_midnight,
            background = 0xFF0F172A.toInt(),
            word = 0xFFF8FAFC.toInt(),
            reading = 0xFF38BDF8.toInt(),
            meaning = 0xFFCBD5E1.toInt(),
            type = 0xFF64748B.toInt(),
            chapter = 0xFF475569.toInt(),
            notes = 0xFFF8FAFC.toInt(),
            example = 0xFF34D399.toInt(),
            backHeader = 0xFF38BDF8.toInt()
        ),
        WidgetTheme(
            key = "sakura",
            backgroundRes = R.drawable.widget_background_sakura,
            background = 0xFF2A1B26.toInt(),
            word = 0xFFFFF1F5.toInt(),
            reading = 0xFFF9A8D4.toInt(),
            meaning = 0xFFE5D5DA.toInt(),
            type = 0xFFB0899C.toInt(),
            chapter = 0xFF9A7D8C.toInt(),
            notes = 0xFFFFF1F5.toInt(),
            example = 0xFF86EFAC.toInt(),
            backHeader = 0xFFF9A8D4.toInt()
        )
    )

    fun byKey(key: String?): WidgetTheme =
        all.firstOrNull { it.key == key } ?: all.first { it.key == DEFAULT_KEY }

    fun current(context: Context): WidgetTheme {
        val key = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .getString(PREF_THEME, DEFAULT_KEY)
        return byKey(key)
    }
}