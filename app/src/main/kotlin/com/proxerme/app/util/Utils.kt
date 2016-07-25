package com.proxerme.app.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target

import java.util.concurrent.ExecutionException

/**
 * Class, which holds various util methods.

 * @author Ruben Gees
 */
object Utils {

    private const val MINIMUM_DIAGONAL_INCHES = 7

    fun areActionsPossible(activity: Activity?): Boolean {
        return activity != null && !activity.isFinishing && !isDestroyedCompat(activity) &&
                !activity.isChangingConfigurations
    }

    fun isDestroyedCompat(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed
    }

    fun isTablet(context: Activity): Boolean {
        val metrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metrics)

        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val diagonalInches = Math.sqrt((xInches * xInches + yInches * yInches).toDouble())

        return diagonalInches >= MINIMUM_DIAGONAL_INCHES
    }

    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun setStatusBarColorIfPossible(activity: Activity?, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            activity?.run {
                window?.statusBarColor = ContextCompat.getColor(activity, color)
            }
        }
    }

    fun calculateSpanAmount(activity: Activity): Int {
        var result = 1

        if (isTablet(activity)) {
            result++
        }

        if (isLandscape(activity)) {
            result++
        }

        return result
    }

    fun getBitmapFromURL(context: Context, url: String): Bitmap? {
        try {
            return Glide.with(context)
                    .load(url)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get()
        } catch (e: InterruptedException) {
            return null
        } catch (e: ExecutionException) {
            return null
        }

    }

}
