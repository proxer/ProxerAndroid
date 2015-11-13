package com.proxerme.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

/**
 * Class, which holds various util methods.
 *
 * @author Ruben Gees
 */
public class Utils {

    private static final int MINIMUM_DIAGONAL_INCHES = 7;

    public static boolean isTablet(@NonNull Activity context) {
        DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float yInches = metrics.heightPixels / metrics.ydpi;
        float xInches = metrics.widthPixels / metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);

        return diagonalInches >= MINIMUM_DIAGONAL_INCHES;
    }

    public static boolean isLandscape(@NonNull Context context) {
        return context.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
    }

    public static int calculateSpanAmount(@NonNull Activity activity) {
        int result = 1;

        if (isTablet(activity)) {
            result++;
        }

        if (isLandscape(activity)) {
            result++;
        }

        return result;
    }

}
