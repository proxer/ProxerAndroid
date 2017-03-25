package me.proxer.app.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import me.proxer.app.R
import me.proxer.app.util.extension.windowManager

/**
 * @author Ruben Gees
 */
object DeviceUtils {

    private const val MINIMUM_DIAGONAL_INCHES = 6.5

    fun isTablet(context: Activity): Boolean {
        val metrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metrics)

        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val diagonalInches = Math.sqrt((xInches * xInches + yInches * yInches).toDouble())

        return diagonalInches >= MINIMUM_DIAGONAL_INCHES
    }

    fun convertDpToPx(context: Context, dp: Float) = (dp * (context.resources.displayMetrics.densityDpi.toFloat() /
            DisplayMetrics.DENSITY_DEFAULT)).toInt()

    fun convertSpToPx(context: Context, sp: Float) = sp * context.resources.displayMetrics.scaledDensity + 0.5f

    fun getScreenWidth(context: Context) = Point().apply { context.windowManager.defaultDisplay.getSize(this) }.x

    fun getVerticalMargin(context: Context) = context.resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)

    fun getHorizontalMargin(context: Context)
            = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)

    fun isLandscape(context: Context)
            = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun calculateSpanAmount(activity: Activity): Int {
        var result = 1

        if (isTablet(activity)) {
            result++
        }

        if (isLandscape(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!activity.isInMultiWindowMode) {
                    result++
                }
            } else {
                result++
            }
        }

        return result
    }
}
