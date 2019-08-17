package me.proxer.app.anime

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings.SettingNotFoundException
import android.provider.Settings.System
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.content.getSystemService
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import io.reactivex.subjects.PublishSubject
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.unsafeLazy
import kotlin.math.abs
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
    val volumeChangeSubject = PublishSubject.create<Int>()
    val brightnessChangeSubject = PublishSubject.create<Int>()

    private val audioManager = requireNotNull(context.getSystemService<AudioManager>())
    private val notificationManager = requireNotNull(context.getSystemService<NotificationManager>())

    private val audioStreamType
        get() = Util.getStreamTypeForAudioUsage(player.audioComponent?.audioAttributes?.usage ?: C.USAGE_MEDIA)

    private val canChangeAudio
        get() = audioManager.isVolumeFixed.not() ||
            if (VERSION.SDK_INT >= VERSION_CODES.M) notificationManager.isNotificationPolicyAccessGranted else true

    private var localVolume = 0f
    private var isScrolling = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent) = true

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            performClick()

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

        override fun onScroll(
            initialEvent: MotionEvent?,
            movingEvent: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            if (
                initialEvent == null ||
                movingEvent == null ||
                // Ignore small swipes.
                abs(movingEvent.y - initialEvent.y) <= 40 ||
                // Ignore horizontal swipes.
                abs(distanceX) > abs(distanceY)
            ) {
                return false
            }

            isScrolling = true

            if (initialEvent.x > width / 2 && canChangeAudio) {
                adjustVolume(distanceY)
            } else {
                adjustBrightness(distanceY)
            }

            return true
        }
    })

    private val settingsChangeObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (!selfChange && !isScrolling) {
                localVolume = audioManager.getStreamVolume(audioStreamType).toFloat()
            }
        }
    }

    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            player.playWhenReady = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        localVolume = audioManager.getStreamVolume(audioStreamType).toFloat()

        context.contentResolver.registerContentObserver(System.CONTENT_URI, true, settingsChangeObserver)
        context.registerReceiver(audioNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onDetachedFromWindow() {
        context.unregisterReceiver(audioNoisyReceiver)
        context.contentResolver.unregisterContentObserver(settingsChangeObserver)

        super.onDetachedFromWindow()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            isScrolling = false
        }

        val horizontalMargin = if (DeviceUtils.isLandscape(resources)) width / 16f else 0f
        val verticalMargin = height / 8f

        val shouldIntercept = (event.x < width / 3 || event.x > width / 3 * 2) &&
            event.x > horizontalMargin && event.x < width - horizontalMargin &&
            event.y > verticalMargin && event.y < height - verticalMargin

        // We only handle events in the left or right third, inside the margins.
        // Delegate other events to the PlayerView.
        return if (shouldIntercept) {
            gestureDetector.onTouchEvent(event)
        } else {
            return super.dispatchTouchEvent(event)
        }
    }

    fun rewind(triggerSubject: Boolean = false) {
        if (player.isCurrentWindowSeekable) {
            player.seekTo(max(player.currentPosition - 10_000, 0))

            if (triggerSubject) {
                rewindSubject.onNext(Unit)
            }
        }
    }

    fun fastForward(triggerSubject: Boolean = false) {
        if (player.isCurrentWindowSeekable) {
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
    }

    private fun adjustVolume(distanceY: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(audioStreamType).toFloat()
        val increment = distanceY / (height / 0.75f) * maxVolume

        localVolume = max(0f, min(maxVolume, localVolume + increment))

        audioManager.setStreamVolume(audioStreamType, localVolume.toInt(), 0)
        volumeChangeSubject.onNext((localVolume / maxVolume * 100).toInt())
    }

    @Suppress("SwallowedException")
    private fun adjustBrightness(distanceY: Float) {
        val increment = distanceY / (height / 0.75f)
        val window = (context as? Activity)?.window
        val windowLayoutParams = window?.attributes

        if (windowLayoutParams != null) {
            val systemBrightness by unsafeLazy {
                try {
                    val value = System.getInt(context.contentResolver, System.SCREEN_BRIGHTNESS)

                    if (value <= 0) 0f else value.toFloat() / 255f
                } catch (_: SettingNotFoundException) {
                    0.5f
                }
            }

            val currentBrightness = windowLayoutParams.screenBrightness.let { if (it < 0) systemBrightness else it }
            val newBrightness = max(0f, min(1f, currentBrightness + increment))

            windowLayoutParams.screenBrightness = newBrightness
            window.attributes = windowLayoutParams

            brightnessChangeSubject.onNext((newBrightness * 100f).toInt())
        }
    }
}
