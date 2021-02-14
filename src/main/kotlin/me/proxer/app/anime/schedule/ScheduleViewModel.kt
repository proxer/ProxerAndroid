package me.proxer.app.anime.schedule

import io.reactivex.Single
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay

/**
 * @author Ruben Gees
 */
class ScheduleViewModel : BaseViewModel<Map<CalendarDay, List<CalendarEntry>>>() {

    override val dataSingle: Single<Map<CalendarDay, List<CalendarEntry>>>
        get() = Single.fromCallable { validate() }
            .flatMap { api.media.calendar().buildSingle() }
            .map { calendarEntries -> calendarEntries.groupBy { it.weekDay } }
            .map { groupedCalendarEntries ->
                groupedCalendarEntries.mapValues { (_, calendarEntries) ->
                    calendarEntries.sortedBy { it.date }
                }
            }
}
