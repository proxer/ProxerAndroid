package com.proxerme.app.util

import android.app.Activity
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.support.annotation.ColorRes
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import cn.nekocode.badge.BadgeDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.library.connection.ProxerException
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.jetbrains.anko.childrenSequence
import org.jetbrains.anko.toast
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

    fun convertSpToPx(context: Context, sp: Float): Float {
        return sp * context.resources.displayMetrics.scaledDensity + 0.5f
    }

    fun getScreenWidth(context: Context): Int {
        return Point().apply { context.windowManager.defaultDisplay.getSize(this) }.x
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

    fun makeMultilineSnackbar(rootView: View, message: CharSequence, duration: Int,
                              maxLines: Int = 5): Snackbar {
        return Snackbar.make(rootView, message, duration)
                .apply {
                    view.childrenSequence().forEach {
                        if (it is TextView && it !is Button) {
                            it.maxLines = maxLines
                        }
                    }
                }
    }

    fun <T> insertAndScrollUpIfNecessary(adapter: PagingAdapter<T>,
                                         layoutManager: RecyclerView.LayoutManager,
                                         recyclerView: RecyclerView,
                                         items: Array<T>) {
        val isFirstDifferent = adapter.items.firstOrNull() != items.firstOrNull()
        val wasAtTop = when (layoutManager) {
            is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition() == 0
            is StaggeredGridLayoutManager -> {
                layoutManager.findFirstVisibleItemPositions(null).contains(0)
            }
            else -> throw IllegalArgumentException("Unknown LayoutManager: $layoutManager")
        }

        adapter.insert(items)

        if (wasAtTop && isFirstDifferent) {
            recyclerView.post {
                recyclerView.smoothScrollToPosition(0)
            }
        }
    }

    fun showError(context: Context, exception: ProxerException, adapter: EasyHeaderFooterAdapter,
                  buttonMessage: String? = null, parent: ViewGroup? = null,
                  onWebClickListener: Link.OnClickListener? = null,
                  onButtonClickListener: View.OnClickListener? = null) {
        showError(context, ErrorHandler.getMessageForErrorCode(context, exception), adapter,
                buttonMessage, parent, onWebClickListener, onButtonClickListener)
    }

    fun showError(context: Context, message: CharSequence, adapter: EasyHeaderFooterAdapter,
                  buttonMessage: String? = null, parent: ViewGroup? = null,
                  onWebClickListener: Link.OnClickListener? = null,
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

    fun <T> populateBadgeView(badgeContainer: ViewGroup, items: Array<T>,
                              transform: (T) -> String,
                              onClick: ((View, T) -> Unit)? = null,
                              textSizeSp: Float = 14f) {
        items.forEach { item ->
            badgeContainer.addView(buildBadgeViewEntry(badgeContainer, item, transform, onClick,
                    textSizeSp))
        }
    }

    fun <T> buildBadgeViewEntry(container: ViewGroup, item: T, transform: (T) -> String,
                                onClick: ((View, T) -> Unit)? = null,
                                textSizeSp: Float = 14f, imageViewToReuse: ImageView? = null):
            ImageView {
        val imageView = imageViewToReuse ?: LayoutInflater.from(container.context)
                .inflate(R.layout.item_badge, container, false) as ImageView

        imageView.setImageDrawable(BadgeDrawable.Builder()
                .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                .badgeColor(ContextCompat.getColor(container.context,
                        R.color.colorAccent))
                .text1(transform.invoke(item))
                .textSize(Utils.convertSpToPx(container.context, textSizeSp))
                .build()
                .apply {
                    setNeedAutoSetBounds(true)
                })

        if (onClick != null) {
            imageView.setOnClickListener {
                onClick.invoke(it, item)
            }
        }

        return imageView
    }

    fun setClipboardContent(activity: Activity, label: String, content: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
        val clip = ClipData.newPlainText(label, content)

        clipboard.primaryClip = clip
    }

    fun viewLink(context: Context, link: String) {
        var uri = Uri.parse(link)

        if (uri.isRelative) {
            uri = uri.buildUpon().scheme("http").build()
        }

        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (exception: ActivityNotFoundException) {
            context.toast(R.string.link_error_not_found)
        }
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
