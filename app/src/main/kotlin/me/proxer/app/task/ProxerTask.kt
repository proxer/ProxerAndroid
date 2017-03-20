package me.proxer.app.task

import com.rubengees.ktask.base.LeafTask
import me.proxer.library.api.ProxerCall

/**
 * @author Ruben Gees
 */
class ProxerTask<O> : LeafTask<ProxerCall<O>, O>() {

    override val isWorking: Boolean
        get() = call != null

    private var call: ProxerCall<O>? = null

    override fun execute(input: ProxerCall<O>) {
        start {
            call = input

            input.enqueue({
                cancel()

                finishSuccessful(it)
            }, {
                cancel()

                finishWithError(it)
            })
        }
    }

    override fun cancel() {
        call?.cancel()
        call = null
    }
}
