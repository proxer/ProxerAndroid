package me.proxer.app.view.bbcode

import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import me.proxer.app.R
import me.proxer.app.util.DeviceUtils
import kotlin.properties.Delegates


/**
 * @author Ruben Gees
 */
class BBSpoilerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var expansionListener: ((hasBeenExpanded: Boolean) -> Unit)? = null
    var expanded by Delegates.observable(false, { _, _, _ ->
        handleExpanded()
    })

    internal val toggle = AppCompatTextView(context)
    internal val decoration = LinearLayout(context)
    internal val space = View(context)
    internal val container = LinearLayout(context)

    init {
        val fourDp = DeviceUtils.convertDpToPx(context, 4f)
        val twoDp = DeviceUtils.convertDpToPx(context, 2f)

        orientation = VERTICAL

        val selectableItemBackground = TypedValue().apply {
            getContext().theme.resolveAttribute(R.attr.selectableItemBackground, this, true)
        }

        TextViewCompat.setTextAppearance(toggle, R.style.TextAppearance_AppCompat_Medium)
        toggle.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        toggle.setBackgroundResource(selectableItemBackground.resourceId)
        toggle.setPadding(fourDp, twoDp, fourDp, twoDp)
        toggle.setOnClickListener {
            expanded = !expanded

            expansionListener?.invoke(expanded)
        }

        decoration.orientation = HORIZONTAL
        space.setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
        container.orientation = VERTICAL

        decoration.addView(space, LinearLayout.LayoutParams(twoDp, MATCH_PARENT).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                marginEnd = fourDp
            } else {
                rightMargin = fourDp
            }
        })
        decoration.addView(container, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

        addView(toggle, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        addView(decoration, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = fourDp
        })

        handleExpanded()
    }

    fun addViews(views: Iterable<View>) {
        views.forEach {
            container.addView(it)
        }
    }

    private fun handleExpanded() {
        if (expanded) {
            decoration.visibility = View.VISIBLE
            toggle.text = context.getString(R.string.view_bbcode_hide_spoiler)
        } else {
            decoration.visibility = View.GONE
            toggle.text = context.getString(R.string.view_bbcode_show_spoiler)
        }
    }
}