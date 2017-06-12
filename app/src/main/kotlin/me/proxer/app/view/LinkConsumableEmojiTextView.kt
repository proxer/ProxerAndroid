package me.proxer.app.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.klinker.android.link_builder.TouchableMovementMethod
import com.vanniktech.emoji.EmojiTextView

/**
 * @author Ruben Gees
 */
class LinkConsumableEmojiTextView : EmojiTextView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        movementMethod.let {
            if (it is TouchableMovementMethod) {
                val span = it.pressedSpan

                if (span != null) {
                    return true
                }
            }
        }

        return false
    }
}