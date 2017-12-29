package me.proxer.app.ui.view.bbcode.tree

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import me.proxer.app.GlideRequests
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype
import me.proxer.app.util.extension.trimEndSafely
import me.proxer.app.util.extension.trimStartSafely

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
            val text = current.text

            current.text = (text as? SpannableStringBuilder)?.trimStartSafely() ?: text.trimStart()
        }

        for (next in views.drop(1)) {
            if (current is TextView && next is TextView) {
                current.append(next.text)
            } else {
                result += current

                current = next
            }
        }

        if (current is TextView) {
            val text = current.text

            current.text = (text as? SpannableStringBuilder)?.trimEndSafely() ?: text.trimEnd()
        }

        result += current

        return result
    }

    protected fun makeViewsWithoutMerging(context: Context) = children.flatMap { it.makeViews(context) }
}
