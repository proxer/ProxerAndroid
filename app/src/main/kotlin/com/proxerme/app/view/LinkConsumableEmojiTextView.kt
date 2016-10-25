package com.proxerme.app.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.klinker.android.link_builder.TouchableMovementMethod
import com.vanniktech.emoji.EmojiTextView

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class LinkConsumableEmojiTextView : EmojiTextView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs,
            defStyle)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        val movementMethod = movementMethod

        if (movementMethod is TouchableMovementMethod) {
            val span = movementMethod.pressedSpan

            if (span != null) {
                return true
            }
        }

        return false
    }
}