package me.proxer.app.util.rx

import androidx.preference.Preference
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.functions.Predicate
import me.proxer.app.util.extension.checkMainThread

/**
 * @author Ruben Gees
 */
class PreferenceClickObservable(
    private val preference: Preference,
    private val handled: Predicate<in Unit>
) : Observable<Unit>() {

    override fun subscribeActual(observer: Observer<in Unit>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(preference, handled, observer)

        observer.onSubscribe(listener)

        preference.onPreferenceClickListener = listener
    }

    internal class Listener(
        private val preference: Preference,
        private val handled: Predicate<in Unit>,
        private val observer: Observer<in Unit>
    ) : MainThreadDisposable(), Preference.OnPreferenceClickListener {

        override fun onPreferenceClick(preference: Preference): Boolean {
            return if (!isDisposed) {
                try {
                    if (handled.test(Unit)) {
                        observer.onNext(Unit)

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
            preference.onPreferenceClickListener = null
        }
    }
}
