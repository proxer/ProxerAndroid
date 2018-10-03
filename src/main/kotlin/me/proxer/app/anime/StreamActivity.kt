package me.proxer.app.anime

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import android.view.View.SYSTEM_UI_FLAG_VISIBLE
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Util
import com.jakewharton.rxbinding2.view.systemUiVisibilityChanges
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.unsafeLazy
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject

/**
 * @author Ruben Gees
 */
class StreamActivity : BaseActivity() {

    private companion object {
        private const val LAST_POSITION_EXTRA = "last_position"
    }

    private val uri
        get() = intent.data ?: throw IllegalStateException("uri is null")

    private val referer: String?
        get() = intent.getStringExtra(StreamResolutionResult.REFERER_EXTRA)

    private val player by unsafeLazy {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)

        player
    }

    private val client by unsafeLazy {
        val client = inject<OkHttpClient>()

        referer.let { referer ->
            if (referer == null) {
                client.value
            } else {
                client.value.newBuilder()
                    .addInterceptor {
                        val requestWithReferer = it.request().newBuilder()
                            .addHeader("Referer", referer)
                            .build()

                        it.proceed(requestWithReferer)
                    }
                    .build()
            }
        }
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val playerView: PlayerView by bindView(R.id.player)

    private var wasPlaying = false

    private var lastPosition: Long
        get() = intent.getLongExtra(LAST_POSITION_EXTRA, -1)
        set(value) {
            intent.putExtra(LAST_POSITION_EXTRA, value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_stream)

        if (savedInstanceState == null) {
            player.playWhenReady = true
        }

        setupUi()
        setupToolbar()
        setupPlayer()
        preparePlayer()
    }

    override fun onResume() {
        super.onResume()

        if (wasPlaying) {
            player.playWhenReady = true
        } else {
            if (player.currentPosition <= 0 && lastPosition > 0) {
                player.seekTo(lastPosition)

                lastPosition = -1
            }
        }

        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onPause() {
        wasPlaying = player.playWhenReady == true && player.playbackState == Player.STATE_READY

        if (player.currentPosition > 0) {
            lastPosition = player.currentPosition
        }

        player.playWhenReady = false

        volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE

        super.onPause()
    }

    override fun onDestroy() {
        player.release()

        super.onDestroy()
    }

    private fun setupUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        window.decorView.systemUiVisibilityChanges()
            .autoDisposable(this.scope())
            .subscribe { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    playerView.showController()
                    toolbar.isVisible = true
                } else {
                    playerView.hideController()
                    toolbar.isGone = true
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }

    private fun setupPlayer() {
        playerView.setControllerVisibilityListener {
            toggleFullscreen(it == View.GONE)
        }

        player.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                if (player.currentPosition > 0) {
                    lastPosition = player.currentPosition
                }

                ErrorUtils.handle(error).let { it ->
                    MaterialDialog(this@StreamActivity)
                        .message(it.message)
                        .positiveButton(R.string.error_action_retry) {
                            preparePlayer()
                        }
                        .negativeButton(R.string.error_action_finish) { finish() }
                        .onCancel { finish() }
                        .show()
                }
            }
        })

        playerView.player = player
    }

    private fun preparePlayer() {
        val okHttpDataSource = OkHttpDataSourceFactory(client, USER_AGENT, DefaultBandwidthMeter())
        val streamType = Util.inferContentType(uri.lastPathSegment)

        val mediaSource = when (streamType) {
            C.TYPE_SS -> SsMediaSource.Factory(DefaultSsChunkSource.Factory(okHttpDataSource), okHttpDataSource)
                .createMediaSource(uri)

            C.TYPE_DASH -> DashMediaSource.Factory(DefaultDashChunkSource.Factory(okHttpDataSource), okHttpDataSource)
                .createMediaSource(uri)

            C.TYPE_HLS -> HlsMediaSource.Factory(okHttpDataSource).createMediaSource(uri)

            C.TYPE_OTHER -> ExtractorMediaSource.Factory(okHttpDataSource).createMediaSource(uri)

            else -> throw IllegalArgumentException("Unknown streamType $streamType")
        }

        player.prepare(mediaSource)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = null
    }

    private fun toggleFullscreen(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = when {
            fullscreen -> SYSTEM_UI_FLAG_LOW_PROFILE or
                SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                SYSTEM_UI_FLAG_FULLSCREEN
            else -> SYSTEM_UI_FLAG_VISIBLE
        }
    }
}
