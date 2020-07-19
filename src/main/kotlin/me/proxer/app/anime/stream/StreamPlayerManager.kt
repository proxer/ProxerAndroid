package me.proxer.app.anime.stream

import android.app.Activity
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.util.DefaultActivityLifecycleCallbacks
import me.proxer.app.util.ErrorUtils
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class StreamPlayerManager(context: StreamActivity, rawClient: OkHttpClient, adTag: Uri?) {

    private companion object {
        private const val WAS_PLAYING_EXTRA = "was_playing"
        private const val LAST_POSITION_EXTRA = "last_position"
    }

    private val weakContext = WeakReference(context)

    private val castSessionAvailabilityListener = object : SessionAvailabilityListener {
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

    private val eventListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING, Player.STATE_IDLE -> playerStateSubject.onNext(PlayerState.LOADING)
                Player.STATE_ENDED -> playerStateSubject.onNext(PlayerState.PAUSING)
                Player.STATE_READY -> playerStateSubject.onNext(
                    when (playWhenReady) {
                        true -> PlayerState.PLAYING
                        false -> PlayerState.PAUSING
                    }
                )
            }
        }

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

                localPlayer.removeListener(eventListener)
                castPlayer?.removeListener(eventListener)

                castPlayer?.setSessionAvailabilityListener(null)

                adsLoader?.release()
                adsLoader = null
            }
        }
    }

    private val client = buildClient(rawClient)

    private val localPlayer = buildLocalPlayer(context)
    private val castPlayer = buildCastPlayer(context)

    private var adsLoader: ImaAdsLoader? = when {
        adTag != null -> ImaAdsLoader(context, adTag).apply {
            setPlayer(localPlayer)
        }
        else -> null
    }

    private var localMediaSource = buildLocalMediaSourceWithAds(client, uri)
    private var castMediaSource = buildCastMediaSource(name, episode, coverUri, uri)

    private val uri get() = requireNotNull(weakContext.get()?.uri)
    private val name: String? get() = weakContext.get()?.name
    private val episode: Int? get() = weakContext.get()?.episode
    private val coverUri: Uri? get() = weakContext.get()?.coverUri
    private val referer: String? get() = weakContext.get()?.referer

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

    val isPlayingAd: Boolean
        get() = localPlayer.isPlayingAd

    val playerReadySubject = BehaviorSubject.createDefault<Player>(localPlayer)
    val playerStateSubject = PublishSubject.create<PlayerState>()
    val errorSubject = PublishSubject.create<ErrorUtils.ErrorAction>()

    init {
        localPlayer.addListener(eventListener)
        castPlayer?.addListener(eventListener)

        localPlayer.prepare(localMediaSource)

        context.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    fun play(position: Long? = null) {
        if (isFirstStart && position != null) {
            lastPosition = position
        }

        if (currentPlayer.currentPosition <= 0 && lastPosition > 0) {
            currentPlayer.seekTo(lastPosition)
        }

        if (isFirstStart || wasPlaying) {
            currentPlayer.playWhenReady = true
        }
    }

    fun pause() {
        wasPlaying = currentPlayer.playWhenReady == true && currentPlayer.playbackState == Player.STATE_READY
        lastPosition = currentPlayer.currentPosition

        localPlayer.playWhenReady = false
    }

    fun toggle() {
        currentPlayer.playWhenReady = currentPlayer.playWhenReady.not()
    }

    fun retry() {
        if (currentPlayer == localPlayer) {
            localPlayer.prepare(localMediaSource, false, false)
        } else if (currentPlayer == castPlayer) {
            castPlayer.loadItem(castMediaSource, lastPosition)
        }
    }

    fun reset() {
        currentPlayer.playWhenReady = false

        wasPlaying = false
        lastPosition = -1

        localMediaSource = buildLocalMediaSourceWithAds(client, uri)
        castMediaSource = buildCastMediaSource(name, episode, coverUri, uri)

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
        val context = requireNotNull(weakContext.get())

        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val okHttpDataSourceFactory = OkHttpDataSourceFactory(client, USER_AGENT, bandwidthMeter)
        val imaFactory = ImaMediaSourceFactory(okHttpDataSourceFactory, this::buildLocalMediaSource)
        val localMediaSource = buildLocalMediaSource(okHttpDataSourceFactory, uri)

        val safeAdsLoader = adsLoader

        return if (safeAdsLoader != null) {
            AdsMediaSource(localMediaSource, imaFactory, safeAdsLoader, context.playerView)
        } else {
            localMediaSource
        }
    }

    private fun buildLocalMediaSource(dataSourceFactory: DataSource.Factory, uri: Uri): MediaSource {
        return when (val streamType = Util.inferContentType(uri)) {
            C.TYPE_SS ->
                SsMediaSource.Factory(DefaultSsChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                    .createMediaSource(uri)

            C.TYPE_DASH ->
                DashMediaSource.Factory(DefaultDashChunkSource.Factory(dataSourceFactory), dataSourceFactory)
                    .createMediaSource(uri)

            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)

            else -> error("Unknown streamType: $streamType")
        }
    }

    private fun buildCastMediaSource(name: String?, episode: Int?, coverUri: Uri?, uri: Uri): MediaQueueItem {
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW).apply {
            if (name != null) {
                putString(MediaMetadata.KEY_TITLE, name)
            }

            if (episode != null) {
                putInt(MediaMetadata.KEY_EPISODE_NUMBER, episode)
            }

            if (coverUri != null) {
                addImage(WebImage(coverUri))
            }
        }

        val mediaInfo = MediaInfo.Builder(uri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.VIDEO_MP4)
            .setMetadata(mediaMetadata)
            .build()

        return MediaQueueItem.Builder(mediaInfo).build()
    }

    private fun buildLocalPlayer(context: StreamActivity): SimpleExoPlayer {
        return SimpleExoPlayer.Builder(context).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()

            setWakeMode(C.WAKE_MODE_NETWORK)
            setHandleAudioBecomingNoisy(true)
            setAudioAttributes(audioAttributes, true)
        }
    }

    private fun buildCastPlayer(context: StreamActivity): CastPlayer? {
        return context.getSafeCastContext()
            ?.let { CastPlayer(it) }
            ?.apply { setSessionAvailabilityListener(castSessionAvailabilityListener) }
    }

    enum class PlayerState {
        PLAYING, PAUSING, LOADING
    }

    private class ImaMediaSourceFactory(
        private val okHttpDataSourceFactory: OkHttpDataSourceFactory,
        private val mediaSourceFunction: (DataSource.Factory, Uri) -> MediaSource
    ) : MediaSourceFactory {

        override fun getSupportedTypes() = intArrayOf(C.TYPE_DASH, C.TYPE_HLS, C.TYPE_OTHER)
        override fun createMediaSource(uri: Uri) = mediaSourceFunction(okHttpDataSourceFactory, uri)
        override fun setDrmSessionManager(drmSessionManager: DrmSessionManager<*>?) = this
    }
}
