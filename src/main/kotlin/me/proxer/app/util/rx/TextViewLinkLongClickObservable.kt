package me.proxer.app.util.rx

import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import me.proxer.app.util.extension.checkMainThread
import me.saket.bettermovementmethod.BetterLinkMovementMethod

/**
 * @author Ruben Gees
 */
class TextViewLinkLongClickObservable(
    private val view: TextView,
    private val handled: (String) -> Boolean
) : Observable<String>() {

    override fun subscribeActual(observer: Observer<in String>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(view, handled, observer)

        observer.onSubscribe(listener)

        view.movementMethod.let {
            if (it is BetterLinkMovementMethod) {
                it.setOnLinkLongClickListener(listener)
            } else {
                view.movementMethod = BetterLinkMovementMethod.newInstance().setOnLinkLongClickListener(listener)
            }
        }
    }

    internal class Listener(
        private val view: TextView,
        private val handled: (String) -> Boolean,
        private val observer: Observer<in String>
    ) : MainThreadDisposable(), BetterLinkMovementMethod.OnLinkLongClickListener {

        override fun onLongClick(textView: TextView, url: String): Boolean {
            return if (!isDisposed) {
                try {
                    if (handled.invoke(url)) {
                        observer.onNext(url)

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
            view.movementMethod.let {
                if (it is BetterLinkMovementMethod) {
                    it.setOnLinkLongClickListener(null)
                }
            }
        }
    }
}
