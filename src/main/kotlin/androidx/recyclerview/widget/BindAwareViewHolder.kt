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

package androidx.recyclerview.widget

import android.view.View

/**
 * A bind-aware {@link RecyclerView.ViewHolder} implementation that knows when it's bound or
 * unbound.
 * <p>
 * Disclaimer: This is in no way supported and THIS COULD BREAK AT ANY TIME. Left for research.
 */
abstract class BindAwareViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun onBind() = Unit

    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun onUnbind() = Unit

    override fun setFlags(flags: Int, mask: Int) {
        val wasBound = isBound

        super.setFlags(flags, mask)

        notifyBinding(wasBound, isBound)
    }

    override fun addFlags(flags: Int) {
        val wasBound = isBound

        super.addFlags(flags)

        notifyBinding(wasBound, isBound)
    }

    override fun clearPayload() {
        val wasBound = isBound

        super.clearPayload()

        notifyBinding(wasBound, isBound)
    }

    override fun resetInternal() {
        val wasBound = isBound

        super.resetInternal()

        notifyBinding(wasBound, isBound)
    }

    private fun notifyBinding(previousBound: Boolean, currentBound: Boolean) {
        if (previousBound && !currentBound) {
            onUnbind()
        } else if (!previousBound && currentBound) {
            onBind()
        }
    }
}
