package com.rubengees.proxerme.util;

import android.content.Context;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class TimeTools {

    public static String convertToRelativeReadableTime(@NonNull Context context, long unixTimeStamp) {
        DateTime time = new DateTime(unixTimeStamp * 1000);
        DateTime currentTime = DateTime.now();

        Days daysBetween = Days.daysBetween(time, currentTime);

        if (daysBetween.getDays() <= 0) {
            Hours hoursBetween = Hours.hoursBetween(time, currentTime);

            if (hoursBetween.getHours() <= 0) {
                Minutes minutesBetween = Minutes.minutesBetween(time, currentTime);

                if (minutesBetween.getMinutes() <= 0) {
                    return "A moment ago";
                } else {
                    return minutesBetween.getMinutes() == 1 ? "One minute ago" :
                            ("" + minutesBetween.getMinutes() + " " + "minutes ago");
                }
            } else {
                return hoursBetween.getHours() == 1 ? "One hour ago" :
                        ("" + hoursBetween.getHours() + " " + "hours ago");
            }
        } else if (daysBetween.getDays() <= 1) {
            return "Yesterday";
        } else if (daysBetween.getDays() <= 30) {
            return "" + daysBetween.getDays() + " " + "days ago";
        } else {
            return "More than one month ago";
        }
    }

}
