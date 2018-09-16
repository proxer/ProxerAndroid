package me.proxer.app.anime.schedule.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import me.proxer.app.util.extension.intentFor

/**
 * @author Ruben Gees
 */
class ScheduleWidgetDarkProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ScheduleWidgetUpdateService.enqueueWork(context, context.intentFor<ScheduleWidgetUpdateService>())
    }
}
