package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.graphics.Typeface.DEFAULT_BOLD
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity.CENTER
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.Gravity.CENTER_VERTICAL
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import me.proxer.app.R
import me.proxer.app.util.extension.setIconicsImage
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
internal class BBSpoilerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var spoilerTitle: String? = null
        set(value) {
            field = value

            handleExpansion()
        }

    private var isExpanded = false
        set(value) {
            field = value

            handleExpansion()

            findHost()?.let { host ->
                if (value) {
                    host.maxHeight = Int.MAX_VALUE
                }

                host.heightChangedListener?.invoke()
            }
        }

    private val toggle = LinearLayout(context)
    private val toggleText = AppCompatTextView(context)
    private val toggleButton = ImageView(context)

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

        toggle.orientation = HORIZONTAL
        container.orientation = VERTICAL
        decoration.orientation = HORIZONTAL

        TextViewCompat.setTextAppearance(toggleText, R.style.TextAppearance_AppCompat_Medium)

        toggle.setOnClickListener { isExpanded = !isExpanded }
        toggle.setBackgroundResource(selectableItemBackground.resourceId)

        toggleText.setPadding(fourDip, twoDip, fourDip, twoDip)
        toggleText.setTag(R.id.ignore_tag, Unit)
        toggleText.typeface = DEFAULT_BOLD
        toggleText.gravity = CENTER

        toggleButton.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)

        space.setBackgroundColor(ContextCompat.getColor(context, R.color.divider))

        toggle.addView(toggleText, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1f).apply {
            gravity = CENTER_VERTICAL
        })

        toggle.addView(toggleButton, LayoutParams(dip(32), dip(32)).apply {
            gravity = CENTER_VERTICAL
        })

        decoration.addView(space, LayoutParams(twoDip, MATCH_PARENT).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                marginEnd = fourDip
            } else {
                rightMargin = fourDip
            }
        })

        decoration.addView(container, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        addView(toggle, LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = CENTER_HORIZONTAL
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
        toggleText.text = when {
            spoilerTitle != null -> spoilerTitle
            isExpanded -> context.getString(R.string.view_bbcode_hide_spoiler)
            else -> context.getString(R.string.view_bbcode_show_spoiler)
        }

        when (isExpanded) {
            true -> ViewCompat.animate(toggleButton).rotation(180f)
            false -> ViewCompat.animate(toggleButton).rotation(0f)
        }
    }

    private fun findHost(): BBCodeView? {
        var current = parent

        while (current !is BBCodeView && current is ViewGroup) {
            current = current.parent
        }

        return current as? BBCodeView
    }
}
