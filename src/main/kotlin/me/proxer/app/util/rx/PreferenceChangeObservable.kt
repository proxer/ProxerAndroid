package me.proxer.app.util.rx

import androidx.preference.Preference
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import me.proxer.app.util.extension.checkMainThread

/**
 * @author Ruben Gees
 */
class PreferenceChangeObservable<T>(
    private val preference: Preference,
    private val handled: (T) -> Boolean
) : Observable<T>() {

    override fun subscribeActual(observer: Observer<in T>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(preference, handled, observer)

        observer.onSubscribe(listener)

        preference.onPreferenceChangeListener = listener
    }

    internal class Listener<T>(
        private val preference: Preference,
        private val handled: (T) -> Boolean,
        private val observer: Observer<in T>
    ) : MainThreadDisposable(), Preference.OnPreferenceChangeListener {

        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            @Suppress("UNCHECKED_CAST")
            newValue as T

            return if (!isDisposed) {
                try {
                    if (handled.invoke(newValue)) {
                        observer.onNext(newValue)

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
            preference.onPreferenceChangeListener = null
        }
    }
}
