package com.proxerme.app.util

import android.content.Context
import com.proxerme.app.R
import org.joda.time.*

/**
 * A helper class to convert a unix timestamp to a human-readable String.

 * @author Ruben Gees
 */
object TimeUtils {

    fun convertToRelativeReadableTime(context: Context, timestamp: Long): String {
        val time = DateTime(timestamp * 1000)
        val currentTime = DateTime.now()

        val yearsBetween = Years.yearsBetween(time, currentTime).years

        if (yearsBetween <= 0) {
            val mothsBetween = Months.monthsBetween(time, currentTime).months

            if (mothsBetween <= 0) {
                val daysBetween = Days.daysBetween(time, currentTime).days

                if (daysBetween <= 0) {
                    val hoursBetween = Hours.hoursBetween(time, currentTime).hours

                    if (hoursBetween <= 0) {
                        val minutesBetween = Minutes.minutesBetween(time, currentTime).minutes

                        if (minutesBetween <= 0) {
                            return context.getString(R.string.time_a_moment_ago)
                        } else {
                            return context.resources.getQuantityString(R.plurals.time_minutes_ago,
                                    minutesBetween, minutesBetween)
                        }
                    } else {
                        return context.resources.getQuantityString(R.plurals.time_hours_ago,
                                hoursBetween, hoursBetween)
                    }
                } else {
                    return context.resources.getQuantityString(R.plurals.time_days_ago,
                            daysBetween, daysBetween)
                }
            } else {
                return context.resources.getQuantityString(R.plurals.time_months_ago,
                        mothsBetween, mothsBetween)
            }
        } else {
            return context.resources.getQuantityString(R.plurals.time_years_ago,
                    yearsBetween, yearsBetween)
        }
    }

}
