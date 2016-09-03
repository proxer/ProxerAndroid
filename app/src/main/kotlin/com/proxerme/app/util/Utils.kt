package com.proxerme.app.util

import adapter.FooterAdapter
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.library.connection.ProxerException
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

    fun buildClickableText(context: Context, text: CharSequence,
                           onWebClickListener: Link.OnClickListener? = null,
                           onWebLongClickListener: Link.OnLongClickListener? = null,
                           onMentionsClickListener: Link.OnClickListener? = null,
                           onMentionsLongClickListener: Link.OnLongClickListener? = null):
            CharSequence {
        val builder = LinkBuilder.from(context, text.toString())

        if (onWebClickListener != null || onWebLongClickListener != null) {
            builder.addLink(Link(WEB_REGEX)
                    .setTextColor(ContextCompat.getColor(context, R.color.colorLink))
                    .setUnderlined(false)
                    .setOnClickListener(onWebClickListener)
                    .setOnLongClickListener(onWebLongClickListener))
        }

        if (onMentionsClickListener != null || onMentionsLongClickListener != null) {
            builder.addLink(Link(MENTIONS_REGEX)
                    .setTextColor(ContextCompat.getColor(context, R.color.colorLink))
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

    fun showError(context: Context, exception: ProxerException, adapter: FooterAdapter,
                  buttonMessage: String? = null, parent: ViewGroup? = null,
                  onWebClickListener: Link.OnClickListener ? = null,
                  onButtonClickListener: View.OnClickListener? = null) {
        showError(context, ErrorHandler.getMessageForErrorCode(context, exception), adapter,
                buttonMessage, parent, onWebClickListener, onButtonClickListener)
    }

    fun showError(context: Context, message: CharSequence, adapter: FooterAdapter,
                  buttonMessage: String? = null, parent: ViewGroup? = null,
                  onWebClickListener: Link.OnClickListener ? = null,
                  onButtonClickListener: View.OnClickListener? = null) {
        val errorContainer = if (adapter.innerAdapter.itemCount <= 0) {
            LayoutInflater.from(context).inflate(R.layout.layout_error, parent, false)
        } else {
            LayoutInflater.from(context).inflate(R.layout.item_error, parent, false)
        }

        val errorText: TextView = errorContainer.findViewById(R.id.errorText) as TextView
        val errorButton: Button = errorContainer.findViewById(R.id.errorButton) as Button

        errorText.movementMethod = TouchableMovementMethod.getInstance()
        errorText.text = Utils.buildClickableText(context, message, onWebClickListener)

        val buttonMessageToSet = buttonMessage ?: context.getString(R.string.error_retry)

        errorButton.text = buttonMessageToSet
        errorButton.setOnClickListener(onButtonClickListener)

        adapter.setFooter(errorContainer)
    }

    fun setClipboardContent(activity: Activity, label: String, content: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
        val clip = ClipData.newPlainText(label, content)

        clipboard.primaryClip = clip
    }

    abstract class OnTextListener : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable) {

        }
    }
}
