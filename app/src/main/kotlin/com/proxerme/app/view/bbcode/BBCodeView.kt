package com.proxerme.app.view.bbcode

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.proxerme.app.R
import java.util.*
import kotlin.properties.Delegates

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class BBCodeView : LinearLayout {

    var bbCode by Delegates.observable<String?>(null, { property, old, new ->
        if (old != new) {
            build()
        }
    })

    var maxHeight = Int.MAX_VALUE

    var expanded by Delegates.observable(true, { property, old, new ->
        if (old != new) {
            invalidate()
        }
    })

    var spoilerStateListener: ((spoilerStates: List<Boolean>) -> Unit)? = null

    private val spoilers = ArrayList<BBSpoiler>()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var modifiableHeightMeasureSpec = heightMeasureSpec
        val hSize = MeasureSpec.getSize(heightMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)

        when (hMode) {
            MeasureSpec.AT_MOST -> modifiableHeightMeasureSpec = MeasureSpec
                    .makeMeasureSpec(Math.min(hSize, maxHeight), MeasureSpec.AT_MOST)

            MeasureSpec.UNSPECIFIED -> modifiableHeightMeasureSpec = MeasureSpec
                    .makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)

            MeasureSpec.EXACTLY -> modifiableHeightMeasureSpec = MeasureSpec
                    .makeMeasureSpec(Math.min(hSize, maxHeight), MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, modifiableHeightMeasureSpec)
    }

    fun getSpoilerStates() = spoilers.map { it.expanded }

    fun setSpoilerStates(states: List<Boolean>?) {
        if (states == null) {
            spoilers.forEach { it.expanded = false }
        } else {
            spoilers.forEachIndexed { index, spoiler ->
                spoiler.expanded = if (states.lastIndex < index) false else states[index]
            }
        }
    }

    fun measureAndGetHeight(): Int {
        val previousMaxHeight = maxHeight
        val previousSpoilerStates = getSpoilerStates()

        maxHeight = Int.MAX_VALUE

        setSpoilerStates(BooleanArray(spoilers.size, { true }).toList())

        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((parent as ViewGroup).measuredWidth,
                View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT,
                View.MeasureSpec.UNSPECIFIED)

        measure(widthMeasureSpec, heightMeasureSpec)

        val result = measuredHeight

        maxHeight = previousMaxHeight
        setSpoilerStates(previousSpoilerStates)

        return result
    }

    private fun build() {
        removeAllViews()
        spoilers.clear()

        if (!bbCode.isNullOrBlank()) {
            buildViews(BBProcessor.process(BBTokenizer.tokenize(bbCode!!))).forEach {
                addView(it)
            }
        }
    }

    private fun buildViews(elements: List<BBProcessor.BBElement>): List<View> {
        val result = LinkedList<View>()

        for (element in elements) {
            if (element is BBProcessor.BBTextElement) {
                val textView = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bbcode_text, this, false) as TextView

                textView.text = element.text
                textView.gravity = element.gravity

                result.add(textView)
            } else if (element is BBProcessor.BBSpoilerElement) {
                val spoiler = BBSpoiler(this).apply {
                    listener = { spoilerStateListener?.invoke(getSpoilerStates()) }

                    addViews(buildViews(element.children))
                }

                spoilers.add(spoiler)
                result.add(spoiler.root)
            }
        }

        return result
    }
}