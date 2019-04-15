package me.proxer.app.settings.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import me.proxer.app.util.extension.dip

/**
 * @author Ruben Gees
 */
class TwoColorBorderDrawable(
    context: Context,
    @ColorInt private val firstColor: Int,
    @ColorInt private val secondColor: Int,
    @ColorInt private val borderColor: Int?
) : Drawable() {

    private val topPaint = Paint().apply {
        color = firstColor
    }

    private val bottomPaint = Paint().apply {
        color = secondColor
    }

    private val selectedIndicatorPaint = borderColor?.let {
        Paint().apply {
            strokeWidth = context.dip(8).toFloat()
            style = Paint.Style.STROKE
            color = it
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds

        val width = (bounds.right - bounds.left).toFloat()
        val height = (bounds.bottom - bounds.top).toFloat()

        canvas.drawRect(0f, 0f, width, height / 2, topPaint)
        canvas.drawRect(0f, height / 2, width, height, bottomPaint)

        if (selectedIndicatorPaint != null) {
            canvas.drawRect(0f, 0f, width, height, selectedIndicatorPaint)
        }
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun getOpacity() = PixelFormat.OPAQUE
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
}
