package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.getMode
import android.view.View.MeasureSpec.getSize
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.FrameLayout
import android.widget.ImageView
import me.proxer.app.GlideRequests
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

    var maxHeight = Int.MAX_VALUE
    var textColor: Int? = null
    var textSize: Int? = null
    var textAppearance: Int? = null
    val spoilerTextColor: Int? = null

    var heightChangedListener: (() -> Unit)? = null
    var glide: GlideRequests? = null
    var userId: String? = null
    var enableEmotions = false

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
        } else {
            removeAllViews()

            tree.makeViews(context, args).forEach { this.addView(it) }
        }
    }
}
