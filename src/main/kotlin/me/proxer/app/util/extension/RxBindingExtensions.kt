@file:Suppress("NOTHING_TO_INLINE")

import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.jakewharton.rxbinding2.support.v7.widget.scrollEvents
import io.reactivex.Observable
import io.reactivex.functions.Predicate
import me.proxer.app.util.extension.safeLayoutManager
import me.proxer.app.util.rx.PreferenceChangeObservable
import me.proxer.app.util.rx.PreferenceClickObservable
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable
import me.proxer.app.util.rx.TextViewLinkClickObservable
import me.proxer.app.util.rx.TextViewLinkLongClickObservable
import me.proxer.app.util.rx.ViewTouchMonitorObservable

@CheckResult
inline fun View.touchesMonitored(handled: Predicate<MotionEvent> = Predicate { true }): Observable<MotionEvent> {
    return ViewTouchMonitorObservable(this, handled)
}

@CheckResult
inline fun TextView.linkClicks(handled: Predicate<String> = Predicate { true }): Observable<String> {
    return TextViewLinkClickObservable(this, handled)
}

@CheckResult
inline fun TextView.linkLongClicks(handled: Predicate<String> = Predicate { true }): Observable<String> {
    return TextViewLinkLongClickObservable(this, handled)
}

@CheckResult
inline fun <T> Preference.changes(handled: Predicate<T> = Predicate { true }): Observable<T> {
    return PreferenceChangeObservable(this, handled)
}

@CheckResult
inline fun Preference.clicks(handled: Predicate<Unit> = Predicate { true }): Observable<Unit> {
    return PreferenceClickObservable(this, handled)
}

@CheckResult
inline fun SubsamplingScaleImageView.events(): Observable<SubsamplingScaleImageViewEventObservable.Event> {
    return SubsamplingScaleImageViewEventObservable(this)
}

@CheckResult
inline fun RecyclerView.endScrolls(threshold: Int = 5): Observable<Unit> = scrollEvents()
    .filter {
        safeLayoutManager.let { safeLayoutManager ->
            val pastVisibleItems = when (safeLayoutManager) {
                is StaggeredGridLayoutManager -> {
                    val visibleItemPositions = IntArray(safeLayoutManager.spanCount).apply {
                        safeLayoutManager.findFirstVisibleItemPositions(this)
                    }

                    when (visibleItemPositions.isNotEmpty()) {
                        true -> visibleItemPositions[0]
                        false -> 0
                    }
                }
                is LinearLayoutManager -> safeLayoutManager.findFirstVisibleItemPosition()
                else -> 0
            }

            safeLayoutManager.itemCount > 0 &&
                safeLayoutManager.childCount + pastVisibleItems >= safeLayoutManager.itemCount - threshold
        }
    }
    .map { Unit }
