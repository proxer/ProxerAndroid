package me.proxer.app.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet

/**
 * @author Ruben Gees
 */
class GifAwareTextView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun invalidateDrawable(drawable: Drawable) {
        invalidate()
    }

    override fun scheduleDrawable(drawable: Drawable, runnable: Runnable, delay: Long) {
        postDelayed(runnable, delay)
    }

    override fun unscheduleDrawable(drawable: Drawable, runnable: Runnable) {
        removeCallbacks(runnable)
    }
}
