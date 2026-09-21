package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object WidgetScheduleHelper {
    private const val PREFS_NAME = "widget_prefs"
    private const val KEY_INTERVAL_MINUTES = "interval_minutes"

    fun getIntervalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERVAL_MINUTES, 60) // default 1 hour (60 minutes)
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_INTERVAL_MINUTES, minutes).apply()
        scheduleWidgetUpdate(context, minutes)
    }

    // Schedules the next rotation alarm using the interval stored in preferences.
    // Called on app launch, widget enable, and device boot so the widget rotates
    // even if the user never opens the settings screen.
    fun ensureScheduled(context: Context) {
        scheduleWidgetUpdate(context, getIntervalMinutes(context))
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, VocabWidgetProvider::class.java).apply {
            action = VocabWidgetProvider.ACTION_CYCLE_WORD
        }
        return PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleWidgetUpdate(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createPendingIntent(context)

        // Cancel existing alarm
        alarmManager.cancel(pendingIntent)

        if (minutes <= 0) {
            Log.d("WidgetScheduleHelper", "Widget scheduled rotation disabled (manual refresh only).")
            return
        }

        val intervalMillis = minutes * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // setAndAllowWhileIdle ensures it fires even in Doze mode
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d("WidgetScheduleHelper", "Scheduled next widget rotation alarm in $minutes minutes.")
        } catch (e: Exception) {
            Log.e("WidgetScheduleHelper", "Failed to schedule widget update alarm", e)
        }
    }

    fun cancelWidgetUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(createPendingIntent(context))
        Log.d("WidgetScheduleHelper", "Cancelled widget rotation alarm.")
    }
}