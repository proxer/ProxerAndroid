package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.getMode
import android.view.View.MeasureSpec.getSize
import android.view.View.MeasureSpec.makeMeasureSpec
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import io.reactivex.subjects.PublishSubject
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.prototype.ImagePrototype.HEIGHT_MAP_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.RootPrototype
import me.proxer.app.ui.view.bbcode.prototype.SpoilerPrototype.SPOILER_EXPAND_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.SpoilerPrototype.SPOILER_TEXT_COLOR_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype.TEXT_APPEARANCE_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype.TEXT_COLOR_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype.TEXT_SIZE_ARGUMENT
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class BBCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val heightChanges: PublishSubject<Unit> = PublishSubject.create()

    @ColorInt
    var textColor: Int? = null

    @Px
    var textSize: Int? = null

    @StyleRes
    var textAppearance: Int? = null

    @ColorInt
    var spoilerTextColor: Int? = null

    var expandSpoilers: Boolean = false

    var maxHeight: Int = Int.MAX_VALUE

    var heightMap: ConcurrentHashMap<String, Int>? = null
    var glide: GlideRequests? = null
    var userId: String? = null
    var enableEmotions = false

    var tree by Delegates.observable<BBTree?>(null) { _, _, new ->
        if (new == null) {
            destroyWithRetainingViews()
            removeAllViews()
        } else if (ViewCompat.isAttachedToWindow(this)) {
            refreshViews(new)
        }
    }

    init {
        if (isInEditMode) {
            EmojiManager.install(IosEmojiProvider())
        }

        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.BBCodeView) {
                textSize = getDimension(R.styleable.BBCodeView_textSize, Float.MIN_VALUE)
                    .let { if (it == Float.MIN_VALUE) null else it.toInt() }

                textAppearance = getResourceId(R.styleable.BBCodeView_textAppearance, Int.MIN_VALUE)
                    .let { if (it == Int.MIN_VALUE) null else it }

                textColor = getColor(R.styleable.BBCodeView_textColor, Int.MIN_VALUE)
                    .let { if (it == Int.MIN_VALUE) null else it }

                spoilerTextColor = getColor(R.styleable.BBCodeView_spoilerTextColor, Int.MIN_VALUE)
                    .let { if (it == Int.MIN_VALUE) null else it }

                maxHeight = getDimensionPixelSize(R.styleable.BBCodeView_maxHeight, Int.MAX_VALUE)

                getString(R.styleable.BBCodeView_text)?.let { tree = it.toSimpleBBTree() }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val hSize = getSize(heightMeasureSpec)
        val hMode = getMode(heightMeasureSpec)

        super.onMeasure(
            widthMeasureSpec,
            when (hMode) {
                AT_MOST -> makeMeasureSpec(min(hSize, maxHeight), AT_MOST)
                EXACTLY -> makeMeasureSpec(min(hSize, maxHeight), EXACTLY)
                UNSPECIFIED -> makeMeasureSpec(maxHeight, AT_MOST)
                else -> error("Illegal measurement mode: $hMode")
            }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        tree?.let { refreshViews(it) }
    }

    fun destroyWithRetainingViews() {
        applyToAllViews(listOf(this)) { view: View ->
            when (view) {
                is ImageView -> glide?.clear(view)
                is WebView -> view.destroy()
            }
        }
    }

    private fun refreshViews(tree: BBTree) {
        val existingChild = if (childCount == 1) children.firstOrNull() else null
        val firstTreeChild = if (tree.children.size == 1) tree.children.firstOrNull() else null

        val args = BBArgs(resources = resources, glide = glide, userId = userId, enableEmoticons = enableEmotions)

        args[TEXT_COLOR_ARGUMENT] = textColor
        args[TEXT_SIZE_ARGUMENT] = textSize
        args[TEXT_APPEARANCE_ARGUMENT] = textAppearance
        args[SPOILER_TEXT_COLOR_ARGUMENT] = spoilerTextColor
        args[SPOILER_EXPAND_ARGUMENT] = expandSpoilers
        args[HEIGHT_MAP_ARGUMENT] = heightMap

        if (existingChild is BetterLinkGifAwareEmojiTextView && firstTreeChild?.prototype === TextPrototype) {
            TextPrototype.applyOnView(this, existingChild, args + firstTreeChild.args)
            RootPrototype.applyOnViews(listOf(existingChild), args + tree.args)

            existingChild.requestLayout()
        } else {
            removeAllViews()

            tree.makeViews(this, args).forEach { this.addView(it) }
        }
    }
}
