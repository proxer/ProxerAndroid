package me.proxer.app.anime.stream

import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer

interface DefaultVideoAdPlayerCallback : VideoAdPlayer.VideoAdPlayerCallback {
    override fun onVolumeChanged(p0: Int) = Unit
    override fun onResume() = Unit
    override fun onPause() = Unit
    override fun onLoaded() = Unit
    override fun onBuffering() = Unit
    override fun onError() = Unit
    override fun onEnded() = Unit
    override fun onPlay() = Unit
}
