package me.proxer.app.ui.view.bbcode

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.Px
import android.support.annotation.StyleRes
import android.util.AttributeSet
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.getMode
import android.view.View.MeasureSpec.getSize
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.FrameLayout
import android.widget.ImageView
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.prototype.RootPrototype
import me.proxer.app.ui.view.bbcode.prototype.SpoilerPrototype.SPOILER_TEXT_COLOR_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype.TEXT_APPEARANCE_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype.TEXT_COLOR_ARGUMENT
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype.TEXT_SIZE_ARGUMENT
import org.jetbrains.anko.childrenSequence

/**
 * @author Ruben Gees
 */
class BBCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @ColorInt
    var textColor: Int?

    @Px
    var textSize: Float?

    @StyleRes
    var textAppearance: Int?

    @ColorInt
    var spoilerTextColor: Int?

    var maxHeight: Int

    var heightChangedListener: (() -> Unit)? = null
    var glide: GlideRequests? = null
    var userId: String? = null
    var enableEmotions = false

    init {
        if (isInEditMode) {
            EmojiManager.install(IosEmojiProvider())
        }

        if (attrs != null) {
            @SuppressLint("Recycle") // False positive.
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BBCodeView)

            textSize = typedArray.getDimension(R.styleable.BBCodeView_textSize, Float.MIN_VALUE)
                .let { if (it == Float.MIN_VALUE) null else it }

            textAppearance = typedArray.getResourceId(R.styleable.BBCodeView_textAppearance, Int.MIN_VALUE)
                .let { if (it == Int.MIN_VALUE) null else it }

            textColor = typedArray.getColor(R.styleable.BBCodeView_textColor, Int.MIN_VALUE)
                .let { if (it == Int.MIN_VALUE) null else it }

            spoilerTextColor = typedArray.getColor(R.styleable.BBCodeView_spoilerTextColor, Int.MIN_VALUE)
                .let { if (it == Int.MIN_VALUE) null else it }

            maxHeight = typedArray.getDimensionPixelSize(R.styleable.BBCodeView_maxHeight, Int.MAX_VALUE)

            typedArray.getString(R.styleable.BBCodeView_text)?.let { setTree(BBParser.parseSimple(it).optimize()) }

            typedArray.recycle()
        } else {
            textColor = null
            textSize = null
            textAppearance = null
            spoilerTextColor = null
            maxHeight = Int.MAX_VALUE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val hSize = getSize(heightMeasureSpec)
        val hMode = getMode(heightMeasureSpec)

        super.onMeasure(widthMeasureSpec, when (hMode) {
            AT_MOST -> makeMeasureSpec(Math.min(hSize, maxHeight), AT_MOST)
            EXACTLY -> makeMeasureSpec(Math.min(hSize, maxHeight), EXACTLY)
            UNSPECIFIED -> makeMeasureSpec(maxHeight, AT_MOST)
            else -> throw IllegalArgumentException("Illegal measurement mode: $hMode")
        })
    }

    fun setTree(tree: BBTree) {
        refreshViews(tree)
    }

    fun destroy() {
        destroyWithRetainingViews()
        removeAllViews()
    }

    fun destroyWithRetainingViews() {
        applyToViews(listOf(this)) { view: ImageView ->
            glide?.clear(view)
        }
    }

    private fun refreshViews(tree: BBTree) {
        val existingChild = if (childCount == 1) this.childrenSequence().firstOrNull() else null
        val firstTreeChild = if (tree.children.size == 1) tree.children.firstOrNull() else null

        val args = BBArgs(glide = glide, userId = userId, enableEmoticons = enableEmotions)

        args[TEXT_COLOR_ARGUMENT] = textColor
        args[TEXT_SIZE_ARGUMENT] = textSize
        args[TEXT_APPEARANCE_ARGUMENT] = textAppearance
        args[SPOILER_TEXT_COLOR_ARGUMENT] = spoilerTextColor

        if (existingChild is BetterLinkGifAwareEmojiTextView && firstTreeChild?.prototype === TextPrototype) {
            TextPrototype.applyOnView(existingChild, args + firstTreeChild.args)
            RootPrototype.applyOnViews(listOf(existingChild), args + tree.args)

            existingChild.requestLayout()
        } else {
            removeAllViews()

            tree.makeViews(context, args).forEach { this.addView(it) }
        }
    }
}
