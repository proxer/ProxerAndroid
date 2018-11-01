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
class PreferenceChangeObservable(
    private val preference: Preference,
    private val handled: Predicate<in String>
) : Observable<String>() {

    override fun subscribeActual(observer: Observer<in String>) {
        if (!observer.checkMainThread()) {
            return
        }

        val listener = Listener(preference, handled, observer)

        observer.onSubscribe(listener)

        preference.onPreferenceChangeListener = listener
    }

    internal class Listener(
        private val preference: Preference,
        private val handled: Predicate<in String>,
        private val observer: Observer<in String>
    ) : MainThreadDisposable(), Preference.OnPreferenceChangeListener {

        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            newValue as String

            return if (!isDisposed) {
                try {
                    if (handled.test(newValue)) {
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
