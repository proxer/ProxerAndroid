package me.proxer.app.view

import android.content.Context
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.TooltipCompat
import android.util.AttributeSet

/**
 * @author Ruben Gees
 */
class InfoImageButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    init {
        contentDescription?.let {
            TooltipCompat.setTooltipText(this, it)
        }
    }
}
