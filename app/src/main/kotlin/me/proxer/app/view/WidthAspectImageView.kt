package me.proxer.app.view

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import me.proxer.app.R

/**
 * An ImageView, which calculates it's height by the given width and an aspect.

 * @author Ruben Gees
 */
class WidthAspectImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private val DEFAULT_ASPECT = 1f
    }

    var aspect: Float

    init {
        if (attrs == null) {
            aspect = DEFAULT_ASPECT
        } else {
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.WidthAspectImageView)

            aspect = attributes.getFloat(R.styleable.WidthAspectImageView_aspect, DEFAULT_ASPECT)

            attributes.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)

        setMeasuredDimension(measuredWidth, (measuredWidth * aspect).toInt())
    }
}
