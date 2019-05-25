package me.proxer.app.anime

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ui.PlayerView
import io.reactivex.subjects.PublishSubject
import kotlin.math.max
import kotlin.math.min

/**
 * @author Ruben Gees
 */
class TouchablePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    val rewindSubject = PublishSubject.create<Unit>()
    val fastForwardSubject = PublishSubject.create<Unit>()

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent) = true

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            delegateTouch(event)

            return true
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            if (event.x > width / 2) {
                fastForward(triggerSubject = true)
            } else {
                rewind(triggerSubject = true)
            }

            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)

        return true
    }

    fun rewind(triggerSubject: Boolean = false) {
        player.seekTo(max(player.currentPosition - 10_000, 0))

        if (triggerSubject) {
            rewindSubject.onNext(Unit)
        }
    }

    fun fastForward(triggerSubject: Boolean = false) {
        val durationMs = player.duration

        val seekPositionMs = if (durationMs != C.TIME_UNSET) {
            min(player.currentPosition + 10_000, durationMs)
        } else {
            player.currentPosition + 10_000
        }

        player.seekTo(seekPositionMs)

        if (triggerSubject) {
            fastForwardSubject.onNext(Unit)
        }
    }

    private fun delegateTouch(event: MotionEvent) {
        super.onTouchEvent(event)
    }
}
