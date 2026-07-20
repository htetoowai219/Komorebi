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
import kotlin.random.Random

class VocabWidgetProvider : AppWidgetProvider() {

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

        if (intent.action == ACTION_CYCLE_WORD || intent.action == ACTION_MANUAL_REFRESH) {
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

        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_CURRENT_ID = "current_vocab_id"
        private const val KEY_CYCLE_MODE = "cycle_mode" // "sequential" or "random"

        suspend fun cycleWord(context: Context) = withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(context).appDao()
            val exposedItems = dao.getExposedItems()
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
                    // Ensure we pick a different word if possible
                    while (nextIndex == currentIndex && exposedItems.size > 1) {
                        nextIndex = Random.nextInt(exposedItems.size)
                    }
                    exposedItems[nextIndex]
                } else {
                    // Sequential rotation
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
            val exposedItems = dao.getExposedItems()

            if (exposedItems.isEmpty()) {
                // Widget empty state styling
                views.setTextViewText(R.id.txt_widget_word, "No words exposed")
                views.setTextViewText(R.id.txt_widget_reading, "Tap to open manager")
                views.setTextViewText(R.id.txt_widget_meaning, "Add vocabulary & chapters, and toggle exposure.")
                views.setViewVisibility(R.id.txt_widget_type, View.GONE)
                views.setViewVisibility(R.id.txt_widget_chapter, View.GONE)
                views.setViewVisibility(R.id.btn_widget_refresh, View.GONE)

                // On empty widget click, redirect user to the app
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

                // Select the active vocabulary card
                var activeItem = exposedItems.find { it.id == currentId }
                if (activeItem == null) {
                    activeItem = exposedItems.first()
                    prefs.edit().putInt(KEY_CURRENT_ID, activeItem.id).apply()
                }

                // Populate widget text fields
                views.setTextViewText(R.id.txt_widget_word, activeItem.word)
                views.setTextViewText(R.id.txt_widget_reading, activeItem.reading)
                views.setTextViewText(R.id.txt_widget_meaning, activeItem.meaning)
                
                views.setViewVisibility(R.id.txt_widget_type, View.VISIBLE)
                views.setViewVisibility(R.id.txt_widget_chapter, View.VISIBLE)
                views.setViewVisibility(R.id.btn_widget_refresh, View.VISIBLE)
                
                views.setTextViewText(R.id.txt_widget_type, activeItem.type.uppercase())

                // Retrieve and bind Chapter details
                val chapter = dao.getChapterById(activeItem.chapterId)
                views.setTextViewText(R.id.txt_widget_chapter, chapter?.name ?: "General")

                // Wire manual refresh action to trigger manual rotation intent
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

                // Wire overall widget background click to launch the Main app
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

            // Commit views update
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
