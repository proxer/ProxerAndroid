package me.proxer.app.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spannable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import me.proxer.app.R
import me.saket.bettermovementmethod.BetterLinkMovementMethod

/**
 * @author Ruben Gees
 */
class BetterLinkTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        // Gross hack to make touch event delegation to parents possible when this view hosts linkified text.
        // See: https://issuetracker.google.com/issues/36908602
        if (movementMethod is BetterLinkMovementMethod) {
            val span = getTag(R.id.bettermovementmethod_highlight_background_span)

            return span != null && (text as? Spannable)?.getSpanStart(span) ?: -1 >= 0
        }

        return false
    }
}
