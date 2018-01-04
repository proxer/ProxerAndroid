package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.view.View
import android.widget.TextView
import me.proxer.app.GlideRequests
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
open class BBTree(val parent: BBTree?, val children: MutableList<BBTree> = mutableListOf()) {

    // Stub for implementation in inheritors. The regexes match nothing and the construct function returns an
    // instance of the BBTree.
    open val prototype = object : BBPrototype {
        override val startRegex = Regex("x^")
        override val endRegex = Regex("x^")
        override fun construct(code: String, parent: BBTree) = BBTree(parent)
    }

    var glide: GlideRequests? = null
        set(value) {
            field = value

            children.forEach {
                it.glide = value
            }
        }

    open fun endsWith(code: String) = prototype.endRegex.matches(code)

    open fun makeViews(context: Context): List<View> {
        val views = children.flatMap { it.makeViews(context) }

        if (views.isEmpty()) return views

        val result = mutableListOf<View>()
        var current = views.first()

        if (current is TextView) {
            current.text = current.text.toSpannableStringBuilder().trimStartSafely()
        }

        for (next in views.drop(1)) {
            if (current is TextView) {
                val currentText = current.text.toSpannableStringBuilder()

                if (next is TextView) {
                    current.text = currentText
                            .apply { if (isEmpty()) insert(0, "\n") }
                            .insert(currentText.length, next.text)
                } else {
                    current.text = currentText
                            .trimEndSafely()
                            .apply { if (isNotEmpty()) insert(length, "\n") }

                    result += current
                    current = next
                }
            } else {
                if (next is TextView) {
                    val nextText = next.text.toSpannableStringBuilder()

                    next.text = nextText
                            .trimStartSafely()
                            .apply { if (isNotEmpty()) insert(0, "\n") }

                    result += current
                } else {
                    result += current
                    result += AppCompatTextView(context).apply {
                        TextViewCompat.setTextAppearance(this, R.style.TextAppearance_AppCompat_Small)
                    }
                }

                current = next
            }
        }

        if (current is TextView) {
            current.text = current.text.toSpannableStringBuilder().trimEndSafely()

            if (current.text.isNotEmpty()) {
                result += current
            }
        } else {
            result += current
        }

        return result
    }

    protected fun makeViewsWithoutMerging(context: Context) = children.flatMap { it.makeViews(context) }
}
