@file:Suppress("NOTHING_TO_INLINE")

import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.CheckResult
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.devbrackets.android.exomedia.ui.widget.VideoView
import io.reactivex.Observable
import io.reactivex.functions.Predicate
import me.proxer.app.util.rx.SubsamplingScaleImageViewEventObservable
import me.proxer.app.util.rx.TextViewLinkClickObservable
import me.proxer.app.util.rx.TextViewLinkLongClickObservable
import me.proxer.app.util.rx.VideoViewErrorObservable
import me.proxer.app.util.rx.VideoViewPreparedObservable
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
inline fun SubsamplingScaleImageView.events(): Observable<SubsamplingScaleImageViewEventObservable.Event> {
    return SubsamplingScaleImageViewEventObservable(this)
}

@CheckResult
inline fun VideoView.preparedEvents(): Observable<Unit> {
    return VideoViewPreparedObservable(this)
}

@CheckResult
inline fun VideoView.errors(handled: Predicate<Exception> = Predicate { true }): Observable<Exception> {
    return VideoViewErrorObservable(this, handled)
}
