package com.proxerme.app.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.proxerme.app.R;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;

/**
 * A helper class to convert a unix timestamp to a human-readable String.
 *
 * @author Ruben Gees
 */
public class TimeUtils {

    public static String convertToRelativeReadableTime(@NonNull Context context,
                                                       long unixTimeStamp) {
        DateTime time = new DateTime(unixTimeStamp * 1000);
        DateTime currentTime = DateTime.now();

        int daysBetween = Days.daysBetween(time, currentTime).getDays();

        if (daysBetween <= 0) {
            int hoursBetween = Hours.hoursBetween(time, currentTime).getHours();

            if (hoursBetween <= 0) {
                int minutesBetween = Minutes.minutesBetween(time, currentTime).getMinutes();

                if (minutesBetween <= 0) {
                    return context.getString(R.string.time_a_moment_ago);
                } else {
                    return context.getResources().getQuantityString(R.plurals.time_minutes_ago,
                            minutesBetween, minutesBetween);
                }
            } else {
                return context.getResources().getQuantityString(R.plurals.time_hours_ago,
                        hoursBetween, hoursBetween);
            }
        } else if (daysBetween <= 30) {
            return context.getResources().getQuantityString(R.plurals.time_days_ago,
                    daysBetween, daysBetween);
        } else {
            return context.getString(R.string.time_more_than_one_month_ago);
        }
    }

}
