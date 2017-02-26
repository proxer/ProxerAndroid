package com.proxerme.app.task

import com.proxerme.app.entitiy.EntryInfo
import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.util.ProxerConnectionWrapper
import com.proxerme.app.util.ProxerConnectionWrapper.ProxerTokenCall
import com.proxerme.library.connection.info.entity.EntryCore
import com.proxerme.library.connection.info.request.EntryCoreRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class EntryInfoTask(private val entryInfoCallback: () -> EntryInfo,
                    successCallback: ((EntryInfo) -> Unit)? = null,
                    exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<String, EntryInfo>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = call != null

    private var call: ProxerTokenCall<EntryCore>? = null

    override fun execute(input: String) {
        val knownInfo = entryInfoCallback.invoke()

        if (knownInfo.isComplete()) {
            finishSuccessful(knownInfo)
        } else {
            start {
                call = ProxerConnectionWrapper.exec(EntryCoreRequest(input), {
                    cancel()

                    finishSuccessful(EntryInfo(it.name, it.episodeAmount))
                }, {
                    cancel()

                    finishWithException(it)
                })
            }
        }
    }

    override fun cancel() {
        call?.cancel()
        call = null
    }

    override fun reset() {
        cancel()
    }
}
