package me.proxer.app.util.rx

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import me.proxer.app.util.extension.checkMainThread
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable.Event

/**
 * @author Ruben Gees
 */
class SubsamplingScaleImageViewEventObservable(private val view: SubsamplingScaleImageView) : Observable<Event>() {

    override fun subscribeActual(observer: Observer<in Event>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(view, observer)

        observer.onSubscribe(listener)

        view.setOnImageEventListener(listener)
    }

    sealed class Event {
        object Ready : Event()
        object Loaded : Event()
        object PreviewReleased : Event()
        class Error(val error: Exception) : Event()
    }

    internal class Listener(
        private val view: SubsamplingScaleImageView,
        private val observer: Observer<in Event>
    ) : MainThreadDisposable(), SubsamplingScaleImageView.OnImageEventListener {

        override fun onReady() {
            if (!isDisposed) {
                try {
                    observer.onNext(Event.Ready)
                } catch (e: Exception) {
                    observer.onError(e)
                    dispose()
                }
            }
        }

        override fun onPreviewReleased() {
            if (!isDisposed) {
                try {
                    observer.onNext(Event.PreviewReleased)
                } catch (e: Exception) {
                    observer.onError(e)
                    dispose()
                }
            }
        }

        override fun onImageLoaded() {
            if (!isDisposed) {
                try {
                    observer.onNext(Event.Loaded)
                } catch (e: Exception) {
                    observer.onError(e)
                    dispose()
                }
            }
        }

        override fun onTileLoadError(error: Exception) {
            emitError(error)
        }

        override fun onImageLoadError(error: Exception) {
            emitError(error)
        }

        override fun onPreviewLoadError(error: Exception) {
            emitError(error)
        }

        override fun onDispose() {
            view.setOnImageEventListener(null)
        }

        private fun emitError(error: Exception) {
            if (!isDisposed) {
                try {
                    observer.onNext(Event.Error(error))
                } catch (e: Exception) {
                    observer.onError(e)
                    dispose()
                }
            }
        }
    }
}
