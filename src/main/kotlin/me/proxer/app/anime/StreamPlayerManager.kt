package me.proxer.app.anime

import android.app.Activity
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import io.reactivex.subjects.PublishSubject
import me.proxer.app.MainApplication
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.util.DefaultActivityLifecycleCallbacks
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.permitSlowCalls
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class StreamPlayerManager(
    context: Activity,
    rawClient: OkHttpClient
) {

    private companion object {
        private const val WAS_PLAYING_EXTRA = "was_playing"
        private const val LAST_POSITION_EXTRA = "last_position"
    }

    val playerReadySubject = PublishSubject.create<Player>()
    val errorSubject = PublishSubject.create<ErrorUtils.ErrorAction>()

    private val weakContext = WeakReference(context)

    private val castSessionAvailabilityListener = object : CastPlayer.SessionAvailabilityListener {
        override fun onCastSessionAvailable() {
            castPlayer.loadItem(castMediaSource, localPlayer.currentPosition)

            currentPlayer = castPlayer
        }

        override fun onCastSessionUnavailable() {
            currentPlayer = localPlayer
        }
    }

    private val errorListener = object : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            lastPosition = currentPlayer.currentPosition

            errorSubject.onNext(ErrorUtils.handle(error))
        }
    }

    private val lifecycleCallbacks = object : DefaultActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (activity == weakContext.get()) {
                isResumed = true
            }
        }

        override fun onActivityPaused(activity: Activity) {
            if (activity == weakContext.get()) {
                isFirstStart = false
                isResumed = false
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activity == weakContext.get()) {
                activity.application.unregisterActivityLifecycleCallbacks(this)

                localPlayer.release()
                castPlayer.release()

                localPlayer.removeListener(errorListener)
                castPlayer.removeListener(errorListener)

                castPlayer.setSessionAvailabilityListener(null)
            }
        }
    }

    private val client = buildClient(rawClient)

    private val localPlayer = buildLocalPlayer(context)
    private val castPlayer = buildCastPlayer(context)

    private val localMediaSource = buildLocalMediaSource(client, uri)
    private val castMediaSource = buildCastMediaSource(name, episode, uri)

    private val uri
        get() = weakContext.get()?.intent?.data ?: throw IllegalStateException("uri is null")

    private val referer: String?
        get() = weakContext.get()?.intent?.getStringExtra(StreamResolutionResult.REFERER_EXTRA)

    private val name: String?
        get() = weakContext.get()?.intent?.getStringExtra(StreamActivity.NAME_EXTRA)

    private val episode: Int?
        get() = weakContext.get()?.intent?.getIntExtra(StreamActivity.EPISODE_EXTRA, -1)?.let {
            if (it <= 0) null else it
        }

    private var lastPosition: Long
        get() = weakContext.get()?.intent?.getLongExtra(LAST_POSITION_EXTRA, -1) ?: -1
        set(value) {
            weakContext.get()?.intent?.putExtra(LAST_POSITION_EXTRA, value)
        }

    private var wasPlaying: Boolean
        get() = weakContext.get()?.intent?.getBooleanExtra(WAS_PLAYING_EXTRA, false) ?: false
        set(value) {
            weakContext.get()?.intent?.putExtra(WAS_PLAYING_EXTRA, value)
        }

    private var isResumed = false
    private var isFirstStart = true

    private var currentPlayer by Delegates.observable<Player>(localPlayer) { _, old, new ->
        old.playWhenReady = false
        new.playWhenReady = isResumed

        new.seekTo(old.currentPosition)

        playerReadySubject.onNext(new)
    }

    init {
        localPlayer.addListener(errorListener)
        castPlayer.addListener(errorListener)

        localPlayer.prepare(localMediaSource)

        context.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    fun resume() {
        if (isFirstStart) {
            currentPlayer.playWhenReady = true

            playerReadySubject.onNext(localPlayer)
        } else if (wasPlaying) {
            currentPlayer.playWhenReady = true
        } else {
            if (currentPlayer.currentPosition <= 0 && lastPosition > 0) {
                currentPlayer.seekTo(lastPosition)
            }
        }
    }

    fun pause() {
        wasPlaying = currentPlayer.playWhenReady == true && currentPlayer.playbackState == Player.STATE_READY
        lastPosition = currentPlayer.currentPosition

        localPlayer.playWhenReady = false
    }

    fun retry() {
        if (currentPlayer == localPlayer) {
            localPlayer.prepare(localMediaSource)
        } else if (currentPlayer == castPlayer) {
            castPlayer.loadItem(castMediaSource, lastPosition)
        }
    }

    private fun buildClient(rawClient: OkHttpClient): OkHttpClient {
        return referer.let { referer ->
            if (referer == null) {
                rawClient
            } else {
                rawClient.newBuilder()
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

    private fun buildCastMediaSource(name: String?, episode: Int?, uri: Uri): MediaQueueItem {
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW).apply {
            if (name != null) {
                putString(MediaMetadata.KEY_TITLE, name)
            }

            if (episode != null) {
                putInt(MediaMetadata.KEY_EPISODE_NUMBER, episode)
            }
        }

        val mediaInfo = MediaInfo.Builder(uri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.VIDEO_UNKNOWN)
            .setMetadata(mediaMetadata)
            .build()

        return MediaQueueItem.Builder(mediaInfo).build()
    }

    private fun buildLocalMediaSource(client: OkHttpClient, uri: Uri): MediaSource {
        val okHttpDataSource = OkHttpDataSourceFactory(client, MainApplication.USER_AGENT, DefaultBandwidthMeter())
        val streamType = Util.inferContentType(uri.lastPathSegment)

        return when (streamType) {
            C.TYPE_SS -> SsMediaSource.Factory(DefaultSsChunkSource.Factory(okHttpDataSource), okHttpDataSource)
                .createMediaSource(uri)

            C.TYPE_DASH -> DashMediaSource.Factory(DefaultDashChunkSource.Factory(okHttpDataSource), okHttpDataSource)
                .createMediaSource(uri)

            C.TYPE_HLS -> HlsMediaSource.Factory(okHttpDataSource).createMediaSource(uri)

            C.TYPE_OTHER -> ExtractorMediaSource.Factory(okHttpDataSource).createMediaSource(uri)

            else -> throw IllegalArgumentException("Unknown streamType $streamType")
        }
    }

    private fun buildCastPlayer(context: Activity): CastPlayer {
        val castContext = permitSlowCalls { CastContext.getSharedInstance(context) }

        return CastPlayer(castContext).apply {
            setSessionAvailabilityListener(castSessionAvailabilityListener)
        }
    }

    private fun buildLocalPlayer(context: Activity): SimpleExoPlayer {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        return ExoPlayerFactory.newSimpleInstance(context, trackSelector)
    }
}
