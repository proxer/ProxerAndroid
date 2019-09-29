package me.proxer.app.anime.schedule.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.library.enums.Category
import org.threeten.bp.format.DateTimeFormatter

/**
 * @author Ruben Gees
 */
class ScheduleWidgetViewsFactory(
    private val context: Context,
    private val dark: Boolean,
    private val calendarEntries: List<SimpleCalendarEntry>
) : RemoteViewsService.RemoteViewsFactory {

    private companion object {
        private val hourMinuteDateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    override fun hasStableIds() = true
    override fun getLoadingView() = null

    override fun getItemId(position: Int) = when {
        // Workaround Android bug passing too large positions.
        position <= calendarEntries.lastIndex -> calendarEntries[position].id.toLong()
        else -> -1L
    }

    override fun getViewAt(position: Int): RemoteViews? {
        // Workaround Android bug passing too large positions.
        if (position > calendarEntries.lastIndex) return null

        val layout = if (dark) R.layout.layout_widget_schedule_dark_item else R.layout.layout_widget_schedule_item
        val entry = calendarEntries[position]

        val result = RemoteViews(BuildConfig.APPLICATION_ID, layout)
        val entryIntent = MediaActivity.getIntent(context, entry.entryId, entry.name, Category.ANIME)

        val dateText = entry.date.toLocalDateTime().format(hourMinuteDateTimeFormatter)
        val episodeText = context.getString(R.string.fragment_schedule_episode, entry.episode.toString())

        result.setTextViewText(R.id.date, dateText)
        result.setTextViewText(R.id.name, entry.name)
        result.setTextViewText(R.id.episode, episodeText)
        result.setOnClickFillInIntent(R.id.container, entryIntent)

        return result
    }

    override fun getCount() = calendarEntries.size
    override fun getViewTypeCount() = 1

    override fun onCreate() = Unit
    override fun onDestroy() = Unit
    override fun onDataSetChanged() = Unit
}
