package me.proxer.app.news.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class NewsWidgetDarkProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        NewsWidgetUpdateService.enqueueWork(context, context.intentFor<NewsWidgetUpdateService>())
    }
}
