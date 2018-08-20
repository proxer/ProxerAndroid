package me.proxer.app.ui.view.bbcode.prototype

import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import androidx.core.content.ContextCompat
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.data.StorageHelper
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
object HidePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *hide( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *hide *", REGEX_OPTIONS)

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        val childViews = super.makeViews(parent, children, args)

        return when {
            childViews.isEmpty() -> childViews
            !StorageHelper.isLoggedIn -> listOf(FrameLayout(parent.context).apply {
                val text = parent.context.getString(R.string.view_bbcode_hide_login)

                layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)

                addView(TextPrototype.makeView(parent, args + BBArgs(text = text)).apply {
                    setTag(R.id.ignore_tag, Unit)

                    gravity = CENTER
                })
            })
            childViews.size == 1 -> childViews
            else -> listOf(LinearLayout(parent.context).apply {
                val fourDip = dip(4)

                layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = VERTICAL

                setPadding(fourDip, fourDip, fourDip, fourDip)
                setBackgroundColor(ContextCompat.getColor(parent.context, R.color.selected))

                childViews.forEach { addView(it) }
            })
        }
    }
}
