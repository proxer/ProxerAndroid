@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.content.Context
import me.proxer.app.R
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import java.util.Date

private val zeroDate = Date(0)

fun Date.distanceInWordsToNow(context: Context): String = when (zeroDate) {
    this -> context.getString(R.string.time_unknown)
    else -> toLocalDateTimeBP().distanceInWordsToNow(context)
}

fun LocalDateTime.formattedDistanceTo(other: LocalDateTime): String {
    val duration = Duration.between(this, other)

    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    val seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).seconds

    return "%02d:%02d:%02d:%02d".format(days, hours, minutes, seconds)
}

fun LocalDateTime.distanceInWordsToNow(context: Context): String {
    val now = LocalDateTime.now()
    val period = Period.between(this.toLocalDate(), now.toLocalDate())

    return if (period.years <= 0) {
        if (period.months <= 0) {
            if (period.days <= 0) {
                val hoursBetween = ChronoUnit.HOURS.between(this, now).toInt()

                if (hoursBetween <= 0) {
                    val minutesBetween = ChronoUnit.MINUTES.between(this, now).toInt()

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

inline fun Instant.toLocalDateTime(): LocalDateTime = atZone(ZoneId.systemDefault()).toLocalDateTime()
inline fun Instant.toLocalDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()
inline fun Instant.toDate() = Date(toEpochMilli())

inline fun Date.toLocalDateTimeBP(): LocalDateTime = toInstantBP().toLocalDateTime()
inline fun Date.toInstantBP(): Instant = Instant.ofEpochMilli(time)
