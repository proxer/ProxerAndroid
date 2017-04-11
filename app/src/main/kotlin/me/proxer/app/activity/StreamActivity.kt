package me.proxer.app.activity

import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar.LENGTH_INDEFINITE
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.VideoControls.*
import com.devbrackets.android.exomedia.ui.widget.VideoView
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar

class StreamActivity : MainActivity() {

    private val uri
        get() = intent.data

    private val root: ViewGroup by bindView(R.id.root)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val player: VideoView by bindView(R.id.player)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_stream)

        setupToolbar()
        setupPlayer()

        toggleFullscreen(true)
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
        player.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
        player.setVideoURI(uri)
        player.setOnErrorListener {
            ErrorUtils.handle(this, it).let {
                multilineSnackbar(root, it.message, LENGTH_INDEFINITE, R.string.error_action_retry,
                        View.OnClickListener {
                            player.reset()
                            player.setVideoURI(uri)
                            player.start()
                        })
            }

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
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = if (fullscreen) getFullscreenUiFlags() else SYSTEM_UI_FLAG_VISIBLE
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
        return SYSTEM_UI_FLAG_LOW_PROFILE or SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}
