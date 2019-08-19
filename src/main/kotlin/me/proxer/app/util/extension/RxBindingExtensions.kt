@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.rubensousa.previewseekbar.exoplayer.PreviewTimeBar
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import io.reactivex.Observable
import me.proxer.app.util.rx.PreferenceChangeObservable
import me.proxer.app.util.rx.PreferenceClickObservable
import me.proxer.app.util.rx.PreviewTimeBarRequestObservable
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable
import me.proxer.app.util.rx.TextViewLinkClickObservable
import me.proxer.app.util.rx.TextViewLinkLongClickObservable
import me.proxer.app.util.rx.ViewTouchMonitorObservable

@CheckResult
inline fun View.touchesMonitored(noinline handled: (MotionEvent) -> Boolean = { true }): Observable<MotionEvent> {
    return ViewTouchMonitorObservable(this, handled)
}

@CheckResult
inline fun TextView.linkClicks(noinline handled: (String) -> Boolean = { true }): Observable<String> {
    return TextViewLinkClickObservable(this, handled)
}

@CheckResult
inline fun TextView.linkLongClicks(noinline handled: (String) -> Boolean = { true }): Observable<String> {
    return TextViewLinkLongClickObservable(this, handled)
}

@CheckResult
inline fun <T> Preference.changes(noinline handled: (T) -> Boolean = { true }): Observable<T> {
    return PreferenceChangeObservable(this, handled)
}

@CheckResult
inline fun Preference.clicks(noinline handled: (Unit) -> Boolean = { true }): Observable<Unit> {
    return PreferenceClickObservable(this, handled)
}

@CheckResult
inline fun SubsamplingScaleImageView.events(): Observable<SubsamplingScaleImageViewEventObservable.Event> {
    return SubsamplingScaleImageViewEventObservable(this)
}

@CheckResult
inline fun PreviewTimeBar.loadRequests(): Observable<Long> {
    return PreviewTimeBarRequestObservable(this)
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
