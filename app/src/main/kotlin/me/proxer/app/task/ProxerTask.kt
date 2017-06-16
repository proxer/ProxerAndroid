package me.proxer.app.task

import com.rubengees.ktask.base.LeafTask
import me.proxer.library.api.ProxerCall
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ErrorType

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

            try {
                val result = input.execute()

                internalCancel()

                finishSuccessful(result)
            } catch(error: Throwable) {
                internalCancel()

                if (error !is ProxerException || error.errorType != ErrorType.CANCELLED) {
                    finishWithError(error)
                }
            }
        }
    }

    override fun cancel() {
        super.cancel()

        internalCancel()
    }

    private fun internalCancel() {
        call?.cancel()
        call = null
    }
}
