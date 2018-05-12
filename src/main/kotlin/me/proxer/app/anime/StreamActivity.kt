package me.proxer.app.anime

import android.os.Build
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.VideoControls
import com.devbrackets.android.exomedia.ui.widget.VideoControls.SYSTEM_UI_FLAG_FULLSCREEN
import com.devbrackets.android.exomedia.ui.widget.VideoControls.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import com.devbrackets.android.exomedia.ui.widget.VideoControls.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import com.devbrackets.android.exomedia.ui.widget.VideoControls.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import com.devbrackets.android.exomedia.ui.widget.VideoControls.SYSTEM_UI_FLAG_LAYOUT_STABLE
import com.devbrackets.android.exomedia.ui.widget.VideoControls.SYSTEM_UI_FLAG_LOW_PROFILE
import com.devbrackets.android.exomedia.ui.widget.VideoControls.SYSTEM_UI_FLAG_VISIBLE
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.jakewharton.rxbinding2.view.systemUiVisibilityChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.postDelayedSafely

/**
 * @author Ruben Gees
 */
class StreamActivity : BaseActivity() {

    private companion object {
        private const val PREVIOUS_POSITION_EXTRA = "previous_position"
    }

    private val uri
        get() = intent.data

    private var pausedInOnStop = false

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val player: VideoView by bindView(R.id.player)

    private var previousPosition: Long
        get() = intent.getLongExtra(PREVIOUS_POSITION_EXTRA, -1)
        set(value) {
            intent.putExtra(PREVIOUS_POSITION_EXTRA, value)
        }

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
        } else {
            player.setOnPreparedListener {
                player.start()

                if (previousPosition > 0) {
                    player.seekTo(previousPosition)

                    previousPosition = -1
                }
            }
        }
    }

    override fun onStop() {
        if (player.isPlaying) {
            pausedInOnStop = true

            player.pause()
        } else {
            player.setOnPreparedListener(null)
        }

        super.onStop()
    }

    override fun onDestroy() {
        toggleFullscreen(false)

        super.onDestroy()
    }

    private fun setupUi() {
        window.decorView?.let {
            it.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

            it.systemUiVisibilityChanges()
                .autoDispose(this)
                .subscribe { visibility ->
                    if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                        player.showControls()

                        toolbar.postDelayedSafely({
                            it.visibility = View.VISIBLE
                        }, 50)
                    } else {
                        toolbar.postDelayedSafely({
                            it.visibility = View.GONE
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
        (player.videoControlsCore as? VideoControls)?.let {
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
                override fun onControlsShown() {}
                override fun onControlsHidden() = toggleFullscreen(true)
            })
        }

        player.setOnErrorListener {
            if (player.currentPosition > 0) {
                previousPosition = player.currentPosition
            }

            ErrorUtils.handle(it).let {
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

        player.setVideoURI(uri)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = when {
            fullscreen -> SYSTEM_UI_FLAG_LOW_PROFILE or SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_HIDE_NAVIGATION
            else -> SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
