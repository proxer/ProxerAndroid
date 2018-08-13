@file:Suppress("NOTHING_TO_INLINE")

import android.support.annotation.CheckResult
import android.view.MotionEvent
import android.view.View
import io.reactivex.Observable
import io.reactivex.functions.Predicate
import me.proxer.app.util.rx.ViewTouchObservableFixed

@CheckResult
inline fun View.touchesFixed(handled: Predicate<MotionEvent> = Predicate { true }): Observable<MotionEvent> {
    return ViewTouchObservableFixed(this, handled)
}
