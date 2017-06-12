package me.proxer.app.activity

import android.os.Build
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.VideoControls.*
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.bindView


class StreamActivity : MainActivity() {

    private val uri
        get() = intent.data

    private var pausedInOnStop = false

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val player: VideoView by bindView(R.id.player)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_stream)

        setupUi()
        setupToolbar()
        setupPlayer()
    }

    override fun onStart() {
        super.onStart()

        if (pausedInOnStop) {
            player.start()

            pausedInOnStop = false
        }
    }

    override fun onStop() {
        if (player.isPlaying) {
            pausedInOnStop = true

            player.pause()
        }

        super.onStop()
    }

    override fun onDestroy() {
        toggleFullscreen(false)

        super.onDestroy()
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

    private fun setupUi() {
        window.decorView?.let {
            it.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

            it.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    player.showControls()

                    toolbar.postDelayed({
                        toolbar.visibility = View.VISIBLE
                    }, 50)
                } else {
                    toolbar.postDelayed({
                        toolbar.visibility = View.GONE
                    }, 50)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }

    private fun setupPlayer() {
        player.videoControls?.let {
            it.setNextDrawable(IconicsDrawable(this, CommunityMaterial.Icon.cmd_fast_forward)
                    .colorRes(android.R.color.white)
                    .sizeDp(24))
            it.setPreviousDrawable(IconicsDrawable(this, CommunityMaterial.Icon.cmd_rewind)
                    .colorRes(android.R.color.white)
                    .sizeDp(24))

            it.setNextButtonRemoved(false)
            it.setPreviousButtonRemoved(false)
            it.setButtonListener(object : VideoControlsButtonListener {
                override fun onPlayPauseClicked() = false
                override fun onRewindClicked() = false
                override fun onFastForwardClicked() = false

                override fun onNextClicked() = when (player.currentPosition + 15000L >= player.duration) {
                    true -> player.seekTo(player.duration)
                    false -> player.seekTo(player.currentPosition + 15000L)
                }.run { true }

                override fun onPreviousClicked() = when (player.currentPosition - 15000L <= 0L) {
                    true -> player.seekTo(0L)
                    false -> player.seekTo(player.currentPosition - 15000L)
                }.run { true }
            })

            it.setVisibilityListener(object : VideoControlsVisibilityListener {
                override fun onControlsShown() {
                    // Nothing to do here.
                }

                override fun onControlsHidden() = toggleFullscreen(true)
            })
        }

        player.setOnErrorListener {
            ErrorUtils.handle(this, it).let {
                MaterialDialog.Builder(this)
                        .content(it.message)
                        .positiveText(R.string.error_action_retry)
                        .negativeText(R.string.error_action_finish)
                        .onPositive { _, _ ->
                            player.reset()
                            player.setVideoURI(uri)
                        }
                        .onNegative { _, _ ->
                            finish()
                        }
                        .cancelListener {
                            finish()
                        }
                        .show()
            }

            false
        }

        player.setOnPreparedListener {
            player.start()
        }

        player.setVideoURI(uri)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = when {
            fullscreen -> {
                SYSTEM_UI_FLAG_LOW_PROFILE or SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
            else -> SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
