package me.proxer.app.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.content.getSystemService
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
object DeviceUtils {

    fun isTablet(context: Context) = context.resources.getBoolean(R.bool.is_tablet)

    fun isLargeTablet(context: Context) = context.resources.getBoolean(R.bool.is_large_tablet)

    @Suppress("DEPRECATION")
    fun getScreenWidth(context: Context): Int {
        val windowManager = requireNotNull(context.getSystemService<WindowManager>())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

            windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            DisplayMetrics()
                .apply { windowManager.defaultDisplay.getMetrics(this) }
                .widthPixels
        }
    }

    @Suppress("DEPRECATION")
    fun getScreenHeight(context: Context): Int {
        val windowManager = requireNotNull(context.getSystemService<WindowManager>())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

            windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            DisplayMetrics()
                .apply { windowManager.defaultDisplay.getMetrics(this) }
                .heightPixels
        }
    }

    fun getVerticalMargin(context: Context, withItems: Boolean = true) = context.resources.getDimensionPixelSize(
        when (withItems) {
            true -> R.dimen.screen_vertical_margin_with_items
            false -> R.dimen.screen_vertical_margin
        }
    )

    fun getHorizontalMargin(context: Context, withItems: Boolean = true) = context.resources.getDimensionPixelSize(
        when (withItems) {
            true -> R.dimen.screen_horizontal_margin_with_items
            false -> R.dimen.screen_horizontal_margin
        }
    )

    fun isLandscape(resources: Resources) = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun calculateSpanAmount(activity: Activity): Int {
        var result = 1

        if (isTablet(activity)) {
            result++
        }

        if (
            isLandscape(activity.resources) &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !activity.isInMultiWindowMode)
        ) {
            result++
        }

        return result
    }
}
