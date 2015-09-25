package com.rubengees.proxerme.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.rubengees.proxerme.R;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class TimeUtils {

    public static String convertToRelativeReadableTime(@NonNull Context context,
                                                       long unixTimeStamp) {
        DateTime time = new DateTime(unixTimeStamp * 1000);
        DateTime currentTime = DateTime.now();

        Days daysBetween = Days.daysBetween(time, currentTime);

        if (daysBetween.getDays() <= 0) {
            Hours hoursBetween = Hours.hoursBetween(time, currentTime);

            if (hoursBetween.getHours() <= 0) {
                Minutes minutesBetween = Minutes.minutesBetween(time, currentTime);

                if (minutesBetween.getMinutes() <= 0) {
                    return context.getString(R.string.time_a_moment_ago);
                } else {
                    return minutesBetween.getMinutes() == 1 ? context
                            .getString(R.string.time_one_minute_ago) :
                            (context.getString(R.string.time_before) + " "
                                    + minutesBetween.getMinutes() + " " +
                                    context.getString(R.string.time_minutes_ago));
                }
            } else {
                return hoursBetween.getHours() == 1 ? context
                        .getString(R.string.time_one_hour_ago) :
                        (context.getString(R.string.time_before) + " " + hoursBetween.getHours()
                                + " " + context.getString(R.string.time_hours_ago));
            }
        } else if (daysBetween.getDays() <= 1) {
            return context.getString(R.string.time_yesterday);
        } else if (daysBetween.getDays() <= 30) {
            return context.getString(R.string.time_before) + " " + daysBetween.getDays()
                    + " " + context.getString(R.string.time_days_ago);
        } else {
            return context.getString(R.string.time_more_than_one_month_ago);
        }
    }

}
