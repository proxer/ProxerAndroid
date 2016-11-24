package com.proxerme.app.activity

import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View.*
import android.view.WindowManager
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.EMVideoView
import com.proxerme.app.R
import com.proxerme.app.util.bindView

class StreamActivity : AppCompatActivity() {

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val player: EMVideoView by bindView(R.id.player)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null

        player.setBackgroundColor(ContextCompat.getColor(this, R.color.md_black_1000))
        player.setVideoURI(intent.data)
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

    override fun onDestroy() {
        toggleFullscreen(false)

        super.onDestroy()
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = if (fullscreen) getFullscreenUiFlags() else
            SYSTEM_UI_FLAG_VISIBLE
        window.decorView.setOnSystemUiVisibilityChangeListener {
            if (it and SYSTEM_UI_FLAG_FULLSCREEN === 0) {
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
}
