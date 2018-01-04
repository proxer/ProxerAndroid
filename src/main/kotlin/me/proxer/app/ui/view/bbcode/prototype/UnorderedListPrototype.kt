package me.proxer.app.ui.view.bbcode.prototype

import android.content.Context
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.view.Gravity.CENTER
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.VERTICAL
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBTree
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object UnorderedListPrototype : BBPrototype {

    override val startRegex = Regex(" *ul( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *ul *", BBPrototype.REGEX_OPTIONS)

    override fun makeViews(context: Context, children: List<BBTree>, args: Map<String, Any?>): List<View> {
        val childViews = children.filter { it.prototype is ListItemPrototype }.flatMap { it.makeViews(context) }

        return listOf(LinearLayout(context).apply {
            val eightDip = dip(8)

            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            orientation = VERTICAL

            childViews.forEach {
                addView(LinearLayout(context).apply {
                    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    orientation = HORIZONTAL

                    addView(AppCompatTextView(context).apply {
                        TextViewCompat.setTextAppearance(this, R.style.TextAppearance_AppCompat_Small)

                        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                            setMargins(0, 0, eightDip, 0)

                            gravity = CENTER
                        }

                        text = "\u2022"
                    })

                    addView(it)
                })
            }
        })
    }
}
