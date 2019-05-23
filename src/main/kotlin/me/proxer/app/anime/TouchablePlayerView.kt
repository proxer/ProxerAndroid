package me.proxer.app.anime

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.exoplayer2.ui.PlayerView

/**
 * @author Ruben Gees
 */
class TouchablePlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private val gestureListener = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            delegateTouch(event)

            return true
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            if (event.x > width / 2) {
                fastForward()
            } else {
                rewind()
            }

            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureListener.onTouchEvent(event)

        return true
    }

    private fun fastForward() {
        if (player.isCurrentWindowSeekable) {
            player.seekTo(player.currentPosition + 10_000)
        }
    }

    private fun rewind() {
        if (player.isCurrentWindowSeekable) {
            player.seekTo(player.currentPosition - 10_000)
        }
    }

    private fun delegateTouch(event: MotionEvent) {
        super.onTouchEvent(event)
    }
}
