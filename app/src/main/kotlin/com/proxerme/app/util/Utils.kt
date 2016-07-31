package com.proxerme.app.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Patterns
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import java.util.concurrent.ExecutionException
import java.util.regex.Pattern

/**
 * Class, which holds various util methods.

 * @author Ruben Gees
 */
object Utils {

    private const val MINIMUM_DIAGONAL_INCHES = 7
    private val WEB_REGEX = Patterns.WEB_URL
    private val MENTIONS_REGEX = Pattern.compile("(@[a-zA-Z0-9_]+)")

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

    fun convertDpToPx(context: Context, dp: Float): Int {
        return (dp * (context.resources.displayMetrics.densityDpi.toFloat() /
                DisplayMetrics.DENSITY_DEFAULT)).toInt()
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

    fun buildClickableText(context: Context, text: String,
                           onWebClickListener: Link.OnClickListener? = null,
                           onWebLongClickListener: Link.OnLongClickListener? = null,
                           onMentionsClickListener: Link.OnClickListener? = null,
                           onMentionsLongClickListener: Link.OnLongClickListener? = null):
            CharSequence {
        val builder = LinkBuilder.from(context, text)

        if (onWebClickListener != null || onWebLongClickListener != null) {
            builder.addLink(Link(WEB_REGEX)
                    .setTextColor(Color.BLUE)
                    .setUnderlined(false)
                    .setOnClickListener(onWebClickListener)
                    .setOnLongClickListener(onWebLongClickListener))
        }

        if (onMentionsClickListener != null || onMentionsLongClickListener != null) {
            builder.addLink(Link(MENTIONS_REGEX)
                    .setTextColor(Color.BLUE)
                    .setUnderlined(false)
                    .setOnClickListener(onMentionsClickListener)
                    .setOnLongClickListener(onMentionsLongClickListener))
        }

        var result = builder.build()

        if (result == null) {
            result = text
        }

        return result
    }
}
