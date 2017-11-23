package me.proxer.app.ui.view.bbcode2

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class BBCodeView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var text by Delegates.observable("", { _, _, new ->
        removeAllViewsInLayout()

        val children = BBParser.parse(new).makeViews(context)

        if (children.isNotEmpty()) {
            children.forEach { addView(it) }
        } else {
            requestLayout()
            invalidate()
        }
    })

    init {
        orientation = LinearLayout.VERTICAL
    }
}
