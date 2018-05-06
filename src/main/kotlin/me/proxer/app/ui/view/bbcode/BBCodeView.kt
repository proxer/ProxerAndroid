package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.getMode
import android.view.View.MeasureSpec.getSize
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.ImageView
import android.widget.LinearLayout
import me.proxer.app.GlideRequests
import me.proxer.app.ui.view.GifAwareTextView
import me.proxer.app.ui.view.bbcode.prototype.RootPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import org.jetbrains.anko.childrenSequence

/**
 * @author Ruben Gees
 */
class BBCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var maxHeight = Int.MAX_VALUE

    var heightChangedListener: (() -> Unit)? = null
    var glide: GlideRequests? = null
    var userId: String? = null
    var enableEmotions = false

    init {
        orientation = VERTICAL
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
        applyToViews(listOf(this), { view: ImageView ->
            glide?.clear(view)
        })
    }

    private fun refreshViews(tree: BBTree) {
        val existingChild = this.childrenSequence().firstOrNull()
        val firstTreeChild = if (tree.children.size == 1) tree.children.firstOrNull() else null

        tree.glide = glide
        tree.userId = userId
        tree.enableEmoticons = enableEmotions

        if (existingChild is GifAwareTextView && firstTreeChild?.prototype === TextPrototype) {
            TextPrototype.applyOnView(existingChild, firstTreeChild.args)
            RootPrototype.applyOnViews(listOf(existingChild), tree.args)
        } else {
            removeAllViews()

            tree.makeViews(context).forEach { this.addView(it) }
        }

        tree.enableEmoticons = false
        tree.userId = null
        tree.glide = null
    }
}
