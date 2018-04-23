package me.proxer.app.anime.schedule

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ScheduleViewModel : BaseViewModel<Map<CalendarDay, List<CalendarEntry>>>() {

    override val dataSingle: Single<Map<CalendarDay, List<CalendarEntry>>>
        get() = api.media().calendar().buildSingle()
            .map { calendarEntries -> calendarEntries.groupBy { it.weekDay } }
            .map { groupedCalendarEntries ->
                groupedCalendarEntries.mapValues { (_, calendarEntries) ->
                    calendarEntries.sortedBy { it.date }
                }
            }
}
