package me.proxer.app.anime

import android.app.Activity
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.reactivex.subjects.PublishSubject
import me.proxer.app.BuildConfig
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.util.DefaultActivityLifecycleCallbacks
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.androidUri
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class StreamPlayerManager(context: StreamActivity, rawClient: OkHttpClient, private val isAdEnabled: Boolean) {

    private companion object {
        private const val WAS_PLAYING_EXTRA = "was_playing"
        private const val LAST_POSITION_EXTRA = "last_position"

        private val AD_TAG_URI = HttpUrl.parse(BuildConfig.AD_TAG_URI)?.androidUri()
    }

    val playerReadySubject = PublishSubject.create<Player>()
    val errorSubject = PublishSubject.create<ErrorUtils.ErrorAction>()

    private val weakContext = WeakReference(context)

    private val castSessionAvailabilityListener = object : CastPlayer.SessionAvailabilityListener {
        override fun onCastSessionAvailable() {
            if (castPlayer != null) {
                castPlayer.loadItem(castMediaSource, localPlayer.currentPosition)

                currentPlayer = castPlayer
            }
        }

        override fun onCastSessionUnavailable() {
            currentPlayer = localPlayer

            if (!isResumed) {
                wasPlaying = false
            }
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
                castPlayer?.release()

                localPlayer.removeListener(errorListener)
                castPlayer?.removeListener(errorListener)

                castPlayer?.setSessionAvailabilityListener(null)
            }
        }
    }

    private val client = buildClient(rawClient)

    private val localPlayer = buildLocalPlayer(context)
    private val castPlayer = buildCastPlayer(context)

    private var localMediaSource = buildLocalMediaSourceWithAds(client, uri)
    private var castMediaSource = buildCastMediaSource(name, episode, uri)

    private val uri
        get() = weakContext.get()?.intent?.data ?: throw IllegalStateException("uri is null")

    private val name: String?
        get() = weakContext.get()?.intent?.getStringExtra(StreamResolutionResult.Video.NAME_EXTRA)

    private val episode: Int?
        get() = weakContext.get()?.intent?.getIntExtra(StreamResolutionResult.Video.EPISODE_EXTRA, -1)?.let {
            if (it <= 0) null else it
        }

    private val referer: String?
        get() = weakContext.get()?.intent?.getStringExtra(StreamResolutionResult.Video.REFERER_EXTRA)

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

    var currentPlayer by Delegates.observable<Player>(localPlayer) { _, old, new ->
        old.playWhenReady = false
        new.playWhenReady = isResumed

        new.seekTo(old.currentPosition)

        playerReadySubject.onNext(new)
    }
        private set

    init {
        localPlayer.addListener(errorListener)
        castPlayer?.addListener(errorListener)

        localPlayer.prepare(localMediaSource)

        context.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    fun start() {
        if (currentPlayer.currentPosition <= 0 && lastPosition > 0) {
            currentPlayer.seekTo(lastPosition)
        }

        if (isFirstStart || wasPlaying) {
            currentPlayer.playWhenReady = true

            if (isFirstStart) {
                playerReadySubject.onNext(localPlayer)
            }
        }
    }

    fun stop() {
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

    fun reset() {
        currentPlayer.playWhenReady = false

        wasPlaying = false
        lastPosition = -1

        localMediaSource = buildLocalMediaSourceWithAds(client, uri)
        castMediaSource = buildCastMediaSource(name, episode, uri)

        retry()

        currentPlayer.playWhenReady = true
    }

    private fun buildClient(rawClient: OkHttpClient): OkHttpClient {
        return referer.let { referer ->
            if (referer == null) {
                rawClient
            } else {
                rawClient.newBuilder()
                    .addInterceptor {
                        val requestWithReferer = it.request().newBuilder()
                            .header("Referer", referer)
                            .build()

                        it.proceed(requestWithReferer)
                    }
                    .build()
            }
        }
    }

    private fun buildLocalMediaSourceWithAds(client: OkHttpClient, uri: Uri): MediaSource {
        val okHttpDataSourceFactory = OkHttpDataSourceFactory(client, USER_AGENT, DefaultBandwidthMeter())
        val localMediaSource = buildLocalMediaSource(okHttpDataSourceFactory, uri)
        val context = weakContext.get()

        return if (AD_TAG_URI != null && context != null && isAdEnabled) {
            val adsLoader = ImaAdsLoader(context, AD_TAG_URI).apply {
                setPlayer(localPlayer)
            }

            AdsMediaSource(localMediaSource, okHttpDataSourceFactory, adsLoader, context.playerView)
        } else {
            localMediaSource
        }
    }

    private fun buildLocalMediaSource(dataSourceFactory: DataSource.Factory, uri: Uri): MediaSource {
        val streamType = Util.inferContentType(uri.lastPathSegment)

        return when (streamType) {
            C.TYPE_SS -> SsMediaSource.Factory(DefaultSsChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                .createMediaSource(uri)

            C.TYPE_DASH -> DashMediaSource.Factory(DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                .createMediaSource(uri)

            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            C.TYPE_OTHER -> ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            else -> throw IllegalArgumentException("Unknown streamType: $streamType")
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
            .setContentType(MimeTypes.VIDEO_MP4)
            .setMetadata(mediaMetadata)
            .build()

        return MediaQueueItem.Builder(mediaInfo).build()
    }

    private fun buildLocalPlayer(context: Activity): SimpleExoPlayer {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        return ExoPlayerFactory.newSimpleInstance(context, trackSelector).apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()

            setAudioAttributes(audioAttributes, true)
        }
    }

    private fun buildCastPlayer(context: Activity): CastPlayer? {
        val availabilityResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)

        return if (availabilityResult == ConnectionResult.SUCCESS) {
            val castContext = CastContext.getSharedInstance(context)

            CastPlayer(castContext).apply {
                setSessionAvailabilityListener(castSessionAvailabilityListener)
            }
        } else {
            null
        }
    }
}
