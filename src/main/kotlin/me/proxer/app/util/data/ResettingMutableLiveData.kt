package me.proxer.app.util.data

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Ruben Gees
 */
class ResettingMutableLiveData<T> : MutableLiveData<T>() {

    private val observerAmount = AtomicInteger()
    private val deliveredAmount = AtomicInteger()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(
            owner,
            Observer {
                observer.onChanged(it)

                if (it != null) {
                    deliveredAmount.incrementAndGet()

                    if (deliveredAmount.compareAndSet(observerAmount.get(), 0)) {
                        value = null
                    }
                }
            }
        )

        observerAmount.incrementAndGet()
    }

    override fun removeObserver(observer: Observer<in T>) {
        observerAmount.decrementAndGet()

        super.removeObserver(observer)
    }
}
