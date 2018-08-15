package me.proxer.app.util.rx

import com.devbrackets.android.exomedia.listener.OnPreparedListener
import com.devbrackets.android.exomedia.ui.widget.VideoView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import me.proxer.app.util.extension.checkMainThread

/**
 * @author Ruben Gees
 */
class VideoViewPreparedObservable(private val view: VideoView) : Observable<Unit>() {

    override fun subscribeActual(observer: Observer<in Unit>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(view, observer)

        observer.onSubscribe(listener)
        view.setOnPreparedListener(listener)
    }

    internal class Listener(
        private val view: VideoView,
        private val observer: Observer<in Unit>
    ) : MainThreadDisposable(), OnPreparedListener {

        override fun onPrepared() {
            if (!isDisposed) {
                try {
                    observer.onNext(Unit)
                } catch (e: Exception) {
                    observer.onError(e)
                    dispose()
                }
            }
        }

        override fun onDispose() {
            view.setOnPreparedListener(null)
        }
    }
}
