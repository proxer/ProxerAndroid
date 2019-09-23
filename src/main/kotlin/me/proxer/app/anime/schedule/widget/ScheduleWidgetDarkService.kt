package me.proxer.app.anime.schedule.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.squareup.moshi.Moshi
import me.proxer.app.util.extension.getSafeStringArrayExtra
import me.proxer.app.util.extension.safeInject

/**
 * @author Ruben Gees
 */
class ScheduleWidgetDarkService : RemoteViewsService() {

    companion object {
        const val ARGUMENT_CALENDAR_ENTRIES = "calendar_entries"
    }

    private val moshi by safeInject<Moshi>()

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val calendarEntries = intent.getSafeStringArrayExtra(ARGUMENT_CALENDAR_ENTRIES)
            .mapNotNull { moshi.adapter(SimpleCalendarEntry::class.java).fromJson(it) }

        return ScheduleWidgetViewsFactory(applicationContext, true, calendarEntries)
    }
}
