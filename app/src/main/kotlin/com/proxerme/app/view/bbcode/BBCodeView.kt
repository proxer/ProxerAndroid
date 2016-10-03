package com.proxerme.app.view.bbcode

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.proxerme.app.R
import org.jetbrains.anko.find
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class BBCodeView : LinearLayout {

    var bbCode: String? = null
        set(value) {
            field = value

            build()
        }

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr)

    private fun build() {
        removeAllViews()

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
                        .inflate(R.layout.layout_bbcode_text, this@BBCodeView, false) as TextView

                textView.text = element.text
                textView.gravity = element.gravity

                result.add(textView)
            } else if (element is BBProcessor.BBSpoilerElement) {
                val spoiler = LayoutInflater.from(context)
                        .inflate(R.layout.layout_bbcode_spoiler, this@BBCodeView, false) as ViewGroup
                val spoilerToggle = spoiler.find<TextView>(R.id.spoilerToggle)
                val spoilerDecoration = spoiler.find<ViewGroup>(R.id.spoilerDecoration)
                val spoilerContainer = spoiler.find<ViewGroup>(R.id.spoilerContainer)

                spoilerToggle.setOnClickListener {
                    if (spoilerDecoration.visibility == View.VISIBLE) {
                        spoilerDecoration.visibility = View.GONE
                        spoilerToggle.text = context.getString(R.string.spoiler_show)
                    } else {
                        spoilerDecoration.visibility = View.VISIBLE
                        spoilerToggle.text = context.getString(R.string.spoiler_hide)
                    }
                }

                buildViews(element.children).forEach {
                    spoilerContainer.addView(it)
                }

                result.add(spoiler)
            }
        }

        return result
    }
}