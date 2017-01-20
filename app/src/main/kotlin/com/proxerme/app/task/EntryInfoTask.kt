package com.proxerme.app.task

import com.proxerme.app.application.MainApplication
import com.proxerme.app.entitiy.EntryInfo
import com.proxerme.app.task.framework.BaseTask
import com.proxerme.library.connection.ProxerCall
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

    private var call: ProxerCall? = null

    override fun execute(input: String) {
        val knownInfo = entryInfoCallback.invoke()

        if (knownInfo.isComplete()) {
            finishSuccessful(knownInfo)
        } else {
            start {
                call = MainApplication.proxerConnection.execute(EntryCoreRequest(input), {
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