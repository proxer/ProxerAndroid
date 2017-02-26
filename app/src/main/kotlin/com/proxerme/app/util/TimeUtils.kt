package com.proxerme.app.util

import android.content.Context
import com.proxerme.app.R
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit

/**
 * A helper class to convert a unix timestamp to a human-readable String.

 * @author Ruben Gees
 */
object TimeUtils {

    fun convertToRelativeReadableTime(context: Context, timestamp: Long): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
        val now = LocalDateTime.now()

        val period = Period.between(dateTime.toLocalDate(), now.toLocalDate())

        if (period.years <= 0) {
            if (period.months <= 0) {
                if (period.days <= 0) {
                    val hoursBetween = ChronoUnit.HOURS.between(dateTime, now).toInt()

                    if (hoursBetween <= 0) {
                        val minutesBetween = ChronoUnit.MINUTES.between(dateTime, now).toInt()

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
                            period.days, period.days)
                }
            } else {
                return context.resources.getQuantityString(R.plurals.time_months_ago,
                        period.months, period.months)
            }
        } else {
            return context.resources.getQuantityString(R.plurals.time_years_ago,
                    period.years, period.years)
        }
    }

}
