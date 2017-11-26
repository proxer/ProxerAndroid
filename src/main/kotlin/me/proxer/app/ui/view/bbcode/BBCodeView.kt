package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.util.AttributeSet
import android.util.SparseBooleanArray
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.getMode
import android.view.View.MeasureSpec.getSize
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.LinearLayout
import org.jetbrains.anko.childrenRecursiveSequence
import org.jetbrains.anko.collections.forEachWithIndex
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class BBCodeView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var maxHeight = Int.MAX_VALUE
    var text by Delegates.observable("", { _, _, _ -> refreshViews() })

    var spoilerStates: SparseBooleanArray
        get() = SparseBooleanArray().apply {
            spoilerViews.forEachWithIndex { index, it -> put(index, it.isExpanded) }
        }
        set(value) = spoilerViews.forEachWithIndex { index, it ->
            it.isExpanded = value.get(index, false)
        }

    var spoilerStateListener: ((SparseBooleanArray, isExpanded: Boolean) -> Unit)? = null

    private val spoilerViews = mutableListOf<BBSpoilerView>()

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

    private fun refreshViews() {
        spoilerViews.clear()
        removeAllViews()

        if (text.isNotBlank()) {
            BBParser.parse(text).makeViews(context).forEach {
                if (it is BBSpoilerView) {
                    spoilerViews += it
                }

                it.childrenRecursiveSequence().forEach {
                    if (it is BBSpoilerView) {
                        spoilerViews += it
                    }
                }

                addView(it)
            }

            spoilerViews.forEach {
                it.expansionListener = {
                    spoilerStateListener?.invoke(spoilerStates, it)
                }
            }
        }
    }
}
