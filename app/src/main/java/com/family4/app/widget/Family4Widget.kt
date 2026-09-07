package com.family4.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.family4.app.R
import com.family4.app.ui.main.MainActivity

/**
 * Family4 home-screen widget.
 *
 * Shows:
 * - App branding header
 * - Quick-action buttons: Camera, Chat, Calendar
 * - Tapping the widget or any button opens MainActivity with the relevant nav target
 */
class Family4Widget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_family4)

            // Root tap → open dashboard
            views.setOnClickPendingIntent(R.id.widgetRoot, makePendingIntent(context, "dashboard", 0))

            // Camera button
            views.setOnClickPendingIntent(R.id.widgetBtnCamera,   makePendingIntent(context, "camera",   1))

            // Chat button
            views.setOnClickPendingIntent(R.id.widgetBtnChat,     makePendingIntent(context, "chat",     2))

            // Calendar button
            views.setOnClickPendingIntent(R.id.widgetBtnCalendar, makePendingIntent(context, "calendar", 3))

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun makePendingIntent(context: Context, target: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("voice_nav_target", target)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
