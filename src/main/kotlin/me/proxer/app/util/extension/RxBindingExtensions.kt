@file:Suppress("NOTHING_TO_INLINE")

import android.support.annotation.CheckResult
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.functions.Predicate
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
