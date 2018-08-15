package me.proxer.app.util.rx

import com.devbrackets.android.exomedia.listener.OnErrorListener
import com.devbrackets.android.exomedia.ui.widget.VideoView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.functions.Predicate
import me.proxer.app.util.extension.checkMainThread

/**
 * @author Ruben Gees
 */
class VideoViewErrorObservable(
    private val view: VideoView,
    private val handled: Predicate<in Exception>
) : Observable<Exception>() {

    override fun subscribeActual(observer: Observer<in Exception>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(view, handled, observer)

        observer.onSubscribe(listener)
        view.setOnErrorListener(listener)
    }

    internal class Listener(
        private val view: VideoView,
        private val handled: Predicate<in Exception>,
        private val observer: Observer<in Exception>
    ) : MainThreadDisposable(), OnErrorListener {

        override fun onError(error: Exception): Boolean {
            return if (!isDisposed) {
                try {
                    if (handled.test(error)) {
                        observer.onNext(error)

                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    observer.onError(e)
                    dispose()

                    false
                }
            } else {
                false
            }
        }

        override fun onDispose() {
            view.setOnPreparedListener(null)
        }
    }
}
