package me.proxer.app.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import me.proxer.app.R
import me.proxer.app.util.extension.activityManager
import me.proxer.app.util.extension.windowManager

/**
 * @author Ruben Gees
 */
object DeviceUtils {

    fun isTablet(context: Context) = context.resources.getBoolean(R.bool.is_tablet)

    fun getScreenWidth(context: Context) = Point().apply { context.windowManager.defaultDisplay.getSize(this) }.x

    fun getVerticalMargin(context: Context, withItems: Boolean = true) = context.resources
            .getDimensionPixelSize(when (withItems) {
                true -> R.dimen.screen_vertical_margin_with_items
                false -> R.dimen.screen_vertical_margin
            })

    fun getHorizontalMargin(context: Context, withItems: Boolean = true) = context.resources
            .getDimensionPixelSize(when (withItems) {
                true -> R.dimen.screen_horizontal_margin_with_items
                false -> R.dimen.screen_horizontal_margin
            })

    fun isLandscape(resources: Resources) = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun calculateSpanAmount(activity: Activity): Int {
        var result = 1

        if (isTablet(activity)) {
            result++
        }

        if (isLandscape(activity.resources)) {
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

    fun shouldShowHighQualityImages(context: Context): Boolean {
        val memoryInfo = ActivityManager.MemoryInfo()

        context.activityManager.getMemoryInfo(memoryInfo)

        return memoryInfo.availMem >= 1024 * 1024 * 1024
    }
}
