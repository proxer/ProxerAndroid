/*
 * Copyright (c) 2017. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.proxer.app.base

import android.view.View
import androidx.recyclerview.widget.BindAwareViewHolder
import com.uber.autodispose.lifecycle.CorrespondingEventsFunction
import com.uber.autodispose.lifecycle.LifecycleEndedException
import com.uber.autodispose.lifecycle.LifecycleScopeProvider
import com.uber.autodispose.lifecycle.LifecycleScopes
import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import me.proxer.app.base.AutoDisposeViewHolder.ViewHolderEvent
import me.proxer.app.base.AutoDisposeViewHolder.ViewHolderEvent.BIND
import me.proxer.app.base.AutoDisposeViewHolder.ViewHolderEvent.UNBIND

/**
 * Example implementation of a [ViewHolder] implementation that implements
 * [LifecycleScopeProvider]. This could be useful for cases where you have subscriptions that should be
 * disposed upon unbinding or otherwise aren't overwritten in future binds.
 */
abstract class AutoDisposeViewHolder(
    itemView: View
) : BindAwareViewHolder(itemView), LifecycleScopeProvider<ViewHolderEvent> {

    private val lifecycleEvents by lazy { BehaviorSubject.create<ViewHolderEvent>() }

    override fun onBind() = lifecycleEvents.onNext(BIND)

    override fun onUnbind() = lifecycleEvents.onNext(UNBIND)

    override fun lifecycle(): Observable<ViewHolderEvent> = lifecycleEvents.hide()

    override fun correspondingEvents(): CorrespondingEventsFunction<ViewHolderEvent> = correspondingEvents

    override fun peekLifecycle(): ViewHolderEvent? = lifecycleEvents.value

    override fun requestScope(): CompletableSource {
        return LifecycleScopes.resolveScopeFromLifecycle(this)
    }

    enum class ViewHolderEvent {
        BIND, UNBIND
    }

    companion object {

        private val correspondingEvents = CorrespondingEventsFunction<ViewHolderEvent> { viewHolderEvent ->
            when (viewHolderEvent) {
                BIND -> UNBIND
                else -> throw LifecycleEndedException("Cannot use ViewHolder lifecycle after unbind.")
            }
        }
    }
}
