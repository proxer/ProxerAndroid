package me.proxer.app.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.klinker.android.link_builder.TouchableMovementMethod
import com.vanniktech.emoji.EmojiTextView

/**
 * @author Ruben Gees
 */
class LinkConsumableEmojiTextView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : EmojiTextView(context, attrs) {

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        return movementMethod.let {
            it is TouchableMovementMethod && it.pressedSpan != null
        }
    }
}
