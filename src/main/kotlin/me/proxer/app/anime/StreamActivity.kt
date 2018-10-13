package me.proxer.app.anime

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import android.view.View.SYSTEM_UI_FLAG_VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.jakewharton.rxbinding2.view.systemUiVisibilityChanges
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.permitSlowCalls
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject

/**
 * @author Ruben Gees
 */
class StreamActivity : BaseActivity() {

    companion object {
        const val NAME_EXTRA = "name"
        const val EPISODE_EXTRA = "epsiode"

        private const val LAST_POSITION_EXTRA = "last_position"
    }

    private val uri
        get() = intent.data ?: throw IllegalStateException("uri is null")

    private val referer: String?
        get() = intent.getStringExtra(StreamResolutionResult.REFERER_EXTRA)

    private val name: String?
        get() = intent.getStringExtra(NAME_EXTRA)

    private val episode: Int?
        get() = intent.getIntExtra(EPISODE_EXTRA, -1).let { if (it <= 0) null else it }

    private val player by unsafeLazy {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)

        player
    }

    private val castMediaQueueItem by unsafeLazy {
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW).apply {
            if (name != null) putString(MediaMetadata.KEY_SERIES_TITLE, name)
            if (episode != null) putInt(MediaMetadata.KEY_EPISODE_NUMBER, episode as Int)
        }

        val mediaInfo = MediaInfo.Builder(uri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.VIDEO_UNKNOWN)
            .setMetadata(mediaMetadata)
            .build()

        MediaQueueItem.Builder(mediaInfo).build()
    }

    private val castPlayer by unsafeLazy {
        val context = permitSlowCalls { CastContext.getSharedInstance(this) }
        val result = CastPlayer(context)

        result.setSessionAvailabilityListener(object : CastPlayer.SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
                player.playWhenReady = false
                playerView.player = result

                result.seekTo(result.currentPosition)
                result.loadItem(castMediaQueueItem, 0L)

                playerView.controllerHideOnTouch = false
                playerView.controllerShowTimeoutMs = 0
                playerView.showController()
            }

            override fun onCastSessionUnavailable() {
                player.playWhenReady = true
                playerView.player = player

                player.seekTo(result.currentPosition)

                playerView.controllerHideOnTouch = true
                playerView.controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
            }
        })

        result
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

    private val errorListener = object : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            updateLastPosition()

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
    }

    private val root: ViewGroup by bindView(R.id.root)
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

        // Call lazy getter to init cast player
        castPlayer
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.activity_stream, menu)

        permitSlowCalls {
            CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.action_cast)
        }

        return true
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

        updateLastPosition()

        player.playWhenReady = false

        volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE

        super.onPause()
    }

    override fun onDestroy() {
        player.release()
        castPlayer.release()

        castPlayer.setSessionAvailabilityListener(null)

        super.onDestroy()
    }

    private fun setupUi() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val newInsets = ViewCompat.onApplyWindowInsets(root, insets)

            if (newInsets.isConsumed) {
                return@setOnApplyWindowInsetsListener newInsets
            }

            ViewCompat.dispatchApplyWindowInsets(toolbar, newInsets)

            if (newInsets.isConsumed) newInsets.consumeSystemWindowInsets() else newInsets
        }

        toggleFullscreen(true)

        window.decorView.systemUiVisibilityChanges()
            .autoDisposable(this.scope())
            .subscribe { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    playerView.showController()
                    toolbar.isVisible = true
                } else {
                    playerView.hideController()
                    toolbar.isVisible = false
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

        player.addListener(errorListener)
        castPlayer.addListener(errorListener)

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

        title = name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = Category.ANIME.toEpisodeAppString(this, episode)
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

    private fun updateLastPosition() {
        val newPosition = when (playerView.player) {
            is CastPlayer -> castPlayer.currentPosition
            else -> player.currentPosition
        }

        if (newPosition >= 0) {
            lastPosition = newPosition
        }
    }
}
