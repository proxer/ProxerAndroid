package me.proxer.app.news.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * @author Ruben Gees
 */
class NewsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        NewsWidgetUpdateWorker.enqueueWork()
    }
}
