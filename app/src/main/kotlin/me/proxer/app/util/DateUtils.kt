package me.proxer.app.util

import android.content.Context
import me.proxer.app.R
import me.proxer.app.util.extension.getQuantityString
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import java.util.*

/**
 * @author Ruben Gees
 */
object DateUtils {

    fun convertToRelativeReadableTime(context: Context, date: Date): String {
        val dateTime = convertToDateTime(date)
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

    fun convertToDateTime(date: Date): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.time), ZoneId.systemDefault())
    }
}
