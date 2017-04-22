package me.proxer.app.view

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import me.proxer.app.R

/**
 * An ImageView, which calculates it's height by the given width and an aspect.

 * @author Ruben Gees
 */
class HeightAspectImageView : AppCompatImageView {

    companion object {
        private val DEFAULT_ASPECT = 1f
    }

    var aspect: Float

    constructor(context: Context?) : super(context) {
        aspect = DEFAULT_ASPECT
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.HeightAspectImageView)

        aspect = attributes.getFloat(R.styleable.HeightAspectImageView_aspect, DEFAULT_ASPECT)

        attributes.recycle()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.HeightAspectImageView)

        aspect = attributes.getFloat(R.styleable.HeightAspectImageView_aspect, DEFAULT_ASPECT)

        attributes.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        setMeasuredDimension((measuredHeight * aspect).toInt(), measuredHeight)
    }
}
