package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import me.proxer.app.R
import org.jetbrains.anko.dip
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
internal class BBSpoilerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var isExpanded by Delegates.observable(false, { _, _, new ->
        handleExpansion()

        findHost()?.let { host ->
            if (new) {
                host.maxHeight = Int.MAX_VALUE
            }

            host.heightChangedListener?.invoke()
        }
    })

    private val toggle = AppCompatTextView(context)
    private val decoration = LinearLayout(context)
    private val space = View(context)
    private val container = LinearLayout(context)

    init {
        val fourDip = dip(4)
        val twoDip = dip(2)

        val selectableItemBackground = TypedValue().apply {
            context.theme.resolveAttribute(R.attr.selectableItemBackground, this, true)
        }

        orientation = VERTICAL

        TextViewCompat.setTextAppearance(toggle, R.style.TextAppearance_AppCompat_Medium)

        toggle.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        toggle.setBackgroundResource(selectableItemBackground.resourceId)
        toggle.setPadding(fourDip, twoDip, fourDip, twoDip)
        toggle.setOnClickListener { isExpanded = !isExpanded }

        container.orientation = VERTICAL
        decoration.orientation = HORIZONTAL
        space.setBackgroundColor(ContextCompat.getColor(context, R.color.divider))

        decoration.addView(space, LinearLayout.LayoutParams(twoDip, MATCH_PARENT).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                marginEnd = fourDip
            } else {
                rightMargin = fourDip
            }
        })

        decoration.addView(container, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

        addView(toggle, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        addView(decoration, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = fourDip
        })

        handleExpansion()
    }

    override fun addView(child: View?) {
        container.addView(child)
    }

    private fun handleExpansion() {
        decoration.visibility = if (isExpanded) View.VISIBLE else View.GONE
        toggle.text = context.getString(when (isExpanded) {
            true -> R.string.view_bbcode_hide_spoiler
            false -> R.string.view_bbcode_show_spoiler
        })
    }

    private fun findHost(): BBCodeView? {
        var current = parent

        while (current !is BBCodeView && current is ViewGroup) {
            current = current.parent
        }

        return current as? BBCodeView
    }
}
