package me.proxer.app.util.rx

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.functions.Predicate
import me.proxer.app.util.extension.checkMainThread

class ViewTouchObservableFixed(
    private val view: View,
    private val handled: Predicate<in MotionEvent>
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
        private val handled: Predicate<in MotionEvent>,
        private val observer: Observer<in MotionEvent>
    ) : MainThreadDisposable(), View.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return if (!isDisposed) {
                try {
                    observer.onNext(event)

                    handled.test(event)
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
