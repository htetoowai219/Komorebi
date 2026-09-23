package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.VocabItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random

class VocabWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // First widget placed on the home screen: make sure the rotation alarm is running.
        WidgetScheduleHelper.ensureScheduled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Last widget removed: stop the rotation alarm to save battery.
        WidgetScheduleHelper.cancelWidgetUpdate(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("VocabWidgetProvider", "onReceive Action: ${intent.action}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Alarms are cleared on reboot, so reschedule the widget rotation.
            WidgetScheduleHelper.ensureScheduled(context)
            return
        }

        if (intent.action == ACTION_FLIP_CARD) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isFlipped = prefs.getBoolean("widget_flipped_$appWidgetId", false)
                prefs.edit().putBoolean("widget_flipped_$appWidgetId", !isFlipped).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        updateWidget(context, appWidgetManager, appWidgetId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (intent.action == ACTION_CYCLE_WORD || intent.action == ACTION_MANUAL_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // 1. Cycle the active vocabulary item
                    cycleWord(context)

                    // 2. Trigger UI update across all active widget instances
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, VocabWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    for (appWidgetId in appWidgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId)
                    }

                    // 3. Reschedule the next alarm if triggered by the periodic cycle alarm
                    if (intent.action == ACTION_CYCLE_WORD) {
                        val minutes = WidgetScheduleHelper.getIntervalMinutes(context)
                        WidgetScheduleHelper.scheduleWidgetUpdate(context, minutes)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_CYCLE_WORD = "com.example.widget.ACTION_CYCLE_WORD"
        const val ACTION_MANUAL_REFRESH = "com.example.widget.ACTION_MANUAL_REFRESH"
        const val ACTION_FLIP_CARD = "com.example.widget.ACTION_FLIP_CARD"

        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_CURRENT_ID = "current_vocab_id"
        private const val KEY_CYCLE_MODE = "cycle_mode" // "sequential" or "random"

        private fun getDayOfWeek1to7(): Int {
            val calendar = Calendar.getInstance()
            val javaDay = calendar.get(Calendar.DAY_OF_WEEK)
            return when (javaDay) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }

        private suspend fun getScheduledOrExposedItems(context: Context): List<VocabItem> {
            val dao = AppDatabase.getDatabase(context).appDao()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val scheduleEnabled = prefs.getBoolean("schedule_mode_enabled", false)

            if (scheduleEnabled) {
                val day = getDayOfWeek1to7()
                val assignedChapterId = prefs.getInt("schedule_day_$day", -1)
                if (assignedChapterId != -1) {
                    val chapterItems = dao.getItemsForChapterList(assignedChapterId)
                    val exposedChapterItems = chapterItems.filter { it.isExposed }
                    if (exposedChapterItems.isNotEmpty()) {
                        return exposedChapterItems
                    }
                }
            }
            return dao.getExposedItems()
        }

        suspend fun cycleWord(context: Context) = withContext(Dispatchers.IO) {
            val exposedItems = getScheduledOrExposedItems(context)
            if (exposedItems.isEmpty()) return@withContext

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentId = prefs.getInt(KEY_CURRENT_ID, -1)
            val mode = prefs.getString(KEY_CYCLE_MODE, "sequential")

            val nextItem = if (exposedItems.size <= 1) {
                exposedItems.first()
            } else {
                val currentIndex = exposedItems.indexOfFirst { it.id == currentId }
                if (mode == "random") {
                    var nextIndex = Random.nextInt(exposedItems.size)
                    while (nextIndex == currentIndex && exposedItems.size > 1) {
                        nextIndex = Random.nextInt(exposedItems.size)
                    }
                    exposedItems[nextIndex]
                } else {
                    val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % exposedItems.size
                    exposedItems[nextIndex]
                }
            }

            prefs.edit().putInt(KEY_CURRENT_ID, nextItem.id).apply()
        }

        suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) = withContext(Dispatchers.IO) {
            val views = RemoteViews(context.packageName, R.layout.widget_vocab)

            val dao = AppDatabase.getDatabase(context).appDao()
            val exposedItems = getScheduledOrExposedItems(context)

            if (exposedItems.isEmpty()) {
                views.setTextViewText(R.id.txt_widget_word, context.getString(R.string.widget_no_words))
                views.setTextViewText(R.id.txt_widget_reading, context.getString(R.string.widget_tap_open))
                views.setTextViewText(R.id.txt_widget_meaning, context.getString(R.string.widget_add_hint))
                views.setViewVisibility(R.id.txt_widget_type, View.GONE)
                views.setViewVisibility(R.id.txt_widget_chapter, View.GONE)
                views.setViewVisibility(R.id.btn_widget_refresh, View.GONE)
                views.setViewVisibility(R.id.btn_widget_flip, View.GONE)

                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            } else {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentId = prefs.getInt(KEY_CURRENT_ID, -1)

                var activeItem = exposedItems.find { it.id == currentId }
                if (activeItem == null) {
                    activeItem = exposedItems.first()
                    prefs.edit().putInt(KEY_CURRENT_ID, activeItem.id).apply()
                }

                views.setViewVisibility(R.id.txt_widget_type, View.VISIBLE)
                views.setViewVisibility(R.id.txt_widget_chapter, View.VISIBLE)
                views.setViewVisibility(R.id.btn_widget_refresh, View.VISIBLE)
                views.setViewVisibility(R.id.btn_widget_flip, View.VISIBLE)

                views.setTextViewText(R.id.txt_widget_type, activeItem.type.uppercase())

                val chapter = dao.getChapterById(activeItem.chapterId)
                views.setTextViewText(R.id.txt_widget_chapter, chapter?.name ?: context.getString(R.string.widget_general_chapter))

                val isFlipped = prefs.getBoolean("widget_flipped_$appWidgetId", false)
                if (isFlipped) {
                    views.setViewVisibility(R.id.layout_widget_front, View.GONE)
                    views.setViewVisibility(R.id.layout_widget_back, View.VISIBLE)

                    val headerText = if (activeItem.reading.isNotBlank()) {
                        "${activeItem.word} [${activeItem.reading}]"
                    } else {
                        activeItem.word
                    }
                    views.setTextViewText(R.id.txt_widget_back_header, headerText)

                    if (activeItem.notes.isNotBlank()) {
                        views.setViewVisibility(R.id.txt_widget_notes, View.VISIBLE)
                        views.setTextViewText(R.id.txt_widget_notes, activeItem.notes)
                    } else {
                        views.setViewVisibility(R.id.txt_widget_notes, View.GONE)
                    }

                    if (activeItem.exampleSentence.isNotBlank()) {
                        views.setViewVisibility(R.id.txt_widget_example_label, View.VISIBLE)
                        views.setTextViewText(
                            R.id.txt_widget_example_label,
                            context.getString(R.string.widget_example_label)
                        )
                        views.setViewVisibility(R.id.txt_widget_example, View.VISIBLE)
                        views.setTextViewText(R.id.txt_widget_example, activeItem.exampleSentence)
                    } else {
                        views.setViewVisibility(R.id.txt_widget_example_label, View.GONE)
                        views.setViewVisibility(R.id.txt_widget_example, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.layout_widget_front, View.VISIBLE)
                    views.setViewVisibility(R.id.layout_widget_back, View.GONE)

                    views.setTextViewText(R.id.txt_widget_word, activeItem.word)
                    views.setTextViewText(R.id.txt_widget_reading, activeItem.reading)
                    views.setTextViewText(R.id.txt_widget_meaning, activeItem.meaning)
                }

                val refreshIntent = Intent(context, VocabWidgetProvider::class.java).apply {
                    action = ACTION_MANUAL_REFRESH
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 2000,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

                val flipIntent = Intent(context, VocabWidgetProvider::class.java).apply {
                    action = ACTION_FLIP_CARD
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val flipPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 3000,
                    flipIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_flip, flipPendingIntent)

                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 1000,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            applyTheme(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /** Applies the global widget theme (background + text colors) via RemoteViews. */
        private fun applyTheme(context: Context, views: RemoteViews) {
            val theme = WidgetThemes.current(context)
            views.setInt(R.id.widget_root, "setBackgroundResource", theme.backgroundRes)
            views.setTextColor(R.id.txt_widget_word, theme.word)
            views.setTextColor(R.id.txt_widget_reading, theme.reading)
            views.setTextColor(R.id.txt_widget_meaning, theme.meaning)
            views.setTextColor(R.id.txt_widget_type, theme.type)
            views.setTextColor(R.id.txt_widget_chapter, theme.chapter)
            views.setTextColor(R.id.txt_widget_notes, theme.notes)
            views.setTextColor(R.id.txt_widget_example_label, theme.notes)
            views.setTextColor(R.id.txt_widget_example, theme.example)
            views.setTextColor(R.id.txt_widget_back_header, theme.backHeader)
        }
    }
}
