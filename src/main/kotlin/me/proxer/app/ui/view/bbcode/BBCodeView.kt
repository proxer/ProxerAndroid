package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.*
import android.widget.ImageView
import android.widget.LinearLayout
import me.proxer.app.GlideRequests

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
        applyToViews(listOf(this), { view: ImageView ->
            glide?.clear(view)
        })

        removeAllViews()
    }

    private fun refreshViews(tree: BBTree) {
        removeAllViews()

        tree.glide = glide
        tree.makeViews(context).forEach { this.addView(it) }
        tree.glide = null
    }
}
