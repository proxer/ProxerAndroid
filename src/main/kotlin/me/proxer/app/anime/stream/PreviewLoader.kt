package me.proxer.app.anime.stream

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
object PreviewLoader {

    fun loadFrames(requests: Observable<Long>, sizeCallback: () -> Size, metaData: PreviewMetaData): Flowable<Bitmap> {
        val mediaMetadataRetriever = MediaMetadataRetriever()

        var mediaMetadataRetrieverDisposable: Disposable? = null
        var isMediaMetadataRetrieverReady = false

        fun getFrameAtTime(timeUs: Long, size: Size): Bitmap? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                mediaMetadataRetriever.getScaledFrameAtTime(timeUs, 0, size.width, size.height)
            } else {
                mediaMetadataRetriever.getFrameAtTime(timeUs)
            }
        }

        val loader = Completable
            .fromAction {
                try {
                    mediaMetadataRetriever.setDataSource(metaData.uri.toString(), makeHeaders(metaData))
                } catch (error: Throwable) {
                    // MediaMetadataRetriever throws IllegalArgumentExceptions on some devices due to bugs in the
                    // implementation. Ignore these by rethrowing a generic RuntimeException.
                    throw RuntimeException(error)
                } finally {
                    // MediaMetadataRetriever does not support interruption and hangs when calling release()
                    // while setDataSource is still in progress. Wait for it to finish before calling release().
                    isMediaMetadataRetrieverReady = true

                    if (mediaMetadataRetrieverDisposable?.isDisposed == true) {
                        Completable.fromAction { mediaMetadataRetriever.release() }
                            .subscribeOn(Schedulers.io())
                            .subscribeAndLogErrors()
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .andThen(requests)
            .toFlowable(BackpressureStrategy.LATEST)
            .observeOn(Schedulers.io(), false, 1)
            .map { getFrameAtTime(it * 1000, sizeCallback()).toOptional() }
            .filterSome()
            .doFinally {
                if (isMediaMetadataRetrieverReady) {
                    Completable.fromAction { mediaMetadataRetriever.release() }
                        .subscribeOn(Schedulers.io())
                        .subscribeAndLogErrors()
                }
            }
            .observeOn(AndroidSchedulers.mainThread())

        return PublishSubject.create<Bitmap>()
            .also { result ->
                mediaMetadataRetrieverDisposable = loader.subscribe({
                    result.onNext(it)
                }, {
                    result.onError(it)
                }, {
                    result.onComplete()
                })

                result.doOnDispose { mediaMetadataRetrieverDisposable?.dispose() }
            }
            .toFlowable(BackpressureStrategy.LATEST)
    }

    data class PreviewMetaData(val uri: Uri, val referer: String?, val isProxerStream: Boolean)

    private fun makeHeaders(metaData: PreviewMetaData) = emptyMap<String, String>()
        .let {
            when {
                metaData.referer != null -> it.plus("Referer" to metaData.referer)
                else -> it
            }
        }
        .let {
            when {
                metaData.isProxerStream -> it.plus("User-Agent" to USER_AGENT)
                else -> it.plus("User-Agent" to GENERIC_USER_AGENT)
            }
        }
}
