package me.proxer.app.util.rx

import com.github.rubensousa.previewseekbar.PreviewLoader
import com.github.rubensousa.previewseekbar.exoplayer.PreviewTimeBar
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import me.proxer.app.util.extension.checkMainThread

/**
 * @author Ruben Gees
 */
class PreviewTimeBarRequestObservable(private val view: PreviewTimeBar) : Observable<Long>() {

    override fun subscribeActual(observer: Observer<in Long>) {
        if (!observer.checkMainThread()) {
            return
        }

        val loader = Loader(view, observer)

        observer.onSubscribe(loader)

        view.setPreviewLoader(loader)
    }

    internal class Loader(
        private val view: PreviewTimeBar,
        private val observer: Observer<in Long>
    ) : MainThreadDisposable(), PreviewLoader {

        override fun loadPreview(currentPosition: Long, max: Long) {
            if (!isDisposed) {
                try {
                    observer.onNext(currentPosition)
                } catch (e: Exception) {
                    observer.onError(e)
                    dispose()
                }
            }
        }

        override fun onDispose() {
            view.setPreviewLoader(null)
        }
    }
}
