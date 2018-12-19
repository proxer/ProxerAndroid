package me.proxer.app.util.rx

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import me.proxer.app.util.extension.checkMainThread

/**
 * @author Ruben Gees
 */
class ViewTouchMonitorObservable(
    private val view: View,
    private val handled: (MotionEvent) -> Boolean
) : Observable<MotionEvent>() {

    override fun subscribeActual(observer: Observer<in MotionEvent>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(view, handled, observer)

        observer.onSubscribe(listener)
        view.setOnTouchListener(listener)
    }

    internal class Listener(
        private val view: View,
        private val handled: (MotionEvent) -> Boolean,
        private val observer: Observer<in MotionEvent>
    ) : MainThreadDisposable(), View.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return if (!isDisposed) {
                try {
                    observer.onNext(event)

                    handled.invoke(event)
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
            view.setOnTouchListener(null)
        }
    }
}
