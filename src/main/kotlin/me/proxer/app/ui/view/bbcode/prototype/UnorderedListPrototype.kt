package me.proxer.app.ui.view.bbcode.prototype

import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.extension.dip

/**
 * @author Ruben Gees
 */
object UnorderedListPrototype : BBPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *ul( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *ul *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = children.filter { it.prototype == ListItemPrototype }.flatMap { it.makeViews(parent, args) }

        return listOf(
            LinearLayout(parent.context).apply {
                val eightDip = dip(8)

                layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = VERTICAL

                childViews.forEach {
                    addView(
                        LinearLayout(parent.context).apply {
                            layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            orientation = HORIZONTAL

                            addView(
                                TextPrototype.makeView(parent, args + BBArgs(text = "\u2022")).apply {
                                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                                        setMargins(0, 0, eightDip, 0)

                                        gravity = CENTER
                                    }
                                }
                            )

                            addView(it)
                        }
                    )
                }
            }
        )
    }
}
