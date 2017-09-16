package me.proxer.app.util.extension

import android.content.Context
import me.proxer.app.R
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import java.util.Date

/**
 * @author Ruben Gees
 */

fun Date.convertToRelativeReadableTime(context: Context): String {
    val dateTime = convertToDateTime()
    val now = LocalDateTime.now()
    val period = Period.between(dateTime.toLocalDate(), now.toLocalDate())

    return if (period.years <= 0) {
        if (period.months <= 0) {
            if (period.days <= 0) {
                val hoursBetween = ChronoUnit.HOURS.between(dateTime, now).toInt()

                if (hoursBetween <= 0) {
                    val minutesBetween = ChronoUnit.MINUTES.between(dateTime, now).toInt()

                    if (minutesBetween <= 0) {
                        context.getString(R.string.time_a_moment_ago)
                    } else {
                        context.getQuantityString(R.plurals.time_minutes_ago, minutesBetween)
                    }
                } else {
                    context.getQuantityString(R.plurals.time_hours_ago, hoursBetween)
                }
            } else {
                context.getQuantityString(R.plurals.time_days_ago, period.days)
            }
        } else {
            context.getQuantityString(R.plurals.time_months_ago, period.months)
        }
    } else {
        context.getQuantityString(R.plurals.time_years_ago, period.years)
    }
}

fun Date.convertToDateTime(): LocalDateTime = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
