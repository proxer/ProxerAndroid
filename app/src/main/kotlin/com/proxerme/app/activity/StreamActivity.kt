package com.proxerme.app.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar.LENGTH_INDEFINITE
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.*
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.VideoControls.*
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.proxerme.app.R
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.bindView
import com.proxerme.app.util.extension.multilineSnackbar
import org.jetbrains.anko.longToast

class StreamActivity : MainActivity() {

    private val uri
        get() = intent.data

    private val root: ViewGroup by bindView(R.id.root)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val player: VideoView by bindView(R.id.player)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setContentView(R.layout.activity_stream)

            setupToolbar()
            setupPlayer()

            toggleFullscreen(true)
        } else {
            longToast(R.string.error_player_sdk_too_low)

            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    public override fun onStop() {
        player.pause()

        super.onPause()
    }

    private fun setupPlayer() {
        player.setBackgroundColor(ContextCompat.getColor(this, R.color.md_black_1000))
        player.setOnTouchListener(TouchListener(this))
        player.setVideoURI(uri)
        player.setOnErrorListener {
            val action = ErrorUtils.handle(this, it)

            multilineSnackbar(root, action.message, LENGTH_INDEFINITE, R.string.error_action_retry,
                    View.OnClickListener {
                        player.reset()
                        player.setVideoURI(uri)
                        player.start()
                    })

            false
        }

        player.videoControls?.setVisibilityListener(object : VideoControlsVisibilityListener {
            override fun onControlsShown() {
                toggleFullscreen(false)
            }

            override fun onControlsHidden() {
                toggleFullscreen(true)
            }
        })

        player.setOnPreparedListener {
            player.start()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = if (fullscreen) getFullscreenUiFlags() else
            SYSTEM_UI_FLAG_VISIBLE
        window.decorView.setOnSystemUiVisibilityChangeListener {
            if (it and SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                player.showControls()
            }
        }

        toolbar.visibility = when (fullscreen) {
            true -> GONE
            false -> VISIBLE
        }
    }

    private fun getFullscreenUiFlags(): Int {
        var flags = SYSTEM_UI_FLAG_LOW_PROFILE or SYSTEM_UI_FLAG_HIDE_NAVIGATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            flags = flags or (SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or SYSTEM_UI_FLAG_FULLSCREEN or
                    SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }

        return flags
    }

    private inner class TouchListener(context: Context) : GestureDetector.SimpleOnGestureListener(),
            View.OnTouchListener {

        private val gestureDetector = GestureDetector(context, this)

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(event)

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            when (player.videoControls?.isShown ?: false) {
                true -> player.videoControls?.hideDelayed(0)
                false -> {
                    player.videoControls?.show()

                    if (player.isPlaying) {
                        player.videoControls?.hideDelayed(DEFAULT_CONTROL_HIDE_DELAY.toLong())
                    }
                }
            }

            return true
        }
    }
}
