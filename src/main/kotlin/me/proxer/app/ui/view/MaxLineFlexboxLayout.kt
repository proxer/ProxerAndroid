package me.proxer.app.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import com.google.android.flexbox.FlexboxLayout
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
class MaxLineFlexboxLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FlexboxLayout(context, attrs, defStyleAttr) {

    var maxLines: Int

    private var currentWidth: Int = 0
    private var currentLine: Int = 1
    private var isShowAllButtonEnabled = false

    init {
        if (attrs != null) {
            @SuppressLint("Recycle") // False positive
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaxLineFlexboxLayout)

            maxLines = typedArray.getInt(R.styleable.MaxLineFlexboxLayout_maxLines, Int.MAX_VALUE)

            typedArray.recycle()
        } else {
            maxLines = Int.MAX_VALUE
        }
    }

    override fun removeAllViews() {
        isShowAllButtonEnabled = false
        currentWidth = 0
        currentLine = 1

        super.removeAllViews()
    }

    override fun addView(child: View) {
        if (canAddView(child)) {
            if (currentWidth + child.measuredWidth > width) {
                currentWidth = child.measuredWidth
                currentLine++
            } else {
                currentWidth += child.measuredWidth
            }

            super.addView(child)
        }
    }

    fun canAddView(view: View): Boolean {
        if (width == 0) throw IllegalStateException("Only call this method after this view has been laid out.")

        if (view.measuredWidth == 0) {
            view.measure(makeMeasureSpec(width, AT_MOST), makeMeasureSpec(0, UNSPECIFIED))
        }

        return currentWidth + view.measuredWidth <= width || currentLine + 1 <= maxLines
    }

    fun enableShowAllButton(listener: (View) -> Unit) {
        if (isShowAllButtonEnabled) return

        val container = FrameLayout(context)
        val button = AppCompatButton(context, null, android.R.attr.borderlessButtonStyle)

        button.text = context.getString(R.string.fragment_media_info_show_all)
        button.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
        button.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        button.setOnClickListener { listener(it) }

        container.layoutParams = FlexboxLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            isWrapBefore = true
        }

        container.addView(button)
        super.addView(container)
    }
}
