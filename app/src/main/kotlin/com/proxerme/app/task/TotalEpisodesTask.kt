package com.proxerme.app.task

import com.proxerme.app.application.MainApplication
import com.proxerme.app.task.TotalEpisodesTask.TotalEpisodesResult
import com.proxerme.app.task.framework.BaseTask
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.info.request.ListInfoRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class TotalEpisodesTask(private val totalEpisodeCallback: () -> Int,
                        successCallback: ((TotalEpisodesResult) -> Unit)? = null,
                        exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<String, TotalEpisodesResult>(successCallback, exceptionCallback) {

    override val isWorking: Boolean
        get() = call != null

    private var call: ProxerCall? = null

    override fun execute(input: String) {
        val knownTotalEpisodes = totalEpisodeCallback.invoke()

        if (knownTotalEpisodes > 0) {
            finishSuccessful(TotalEpisodesResult(knownTotalEpisodes))
        } else {
            start {
                call = MainApplication.proxerConnection.execute(ListInfoRequest(input, 0)
                        .withLimit(Int.MAX_VALUE), {
                    cancel()

                    finishSuccessful(TotalEpisodesResult(it.lastEpisode))
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

    /**
     * There seems to be a bug in the Kotlin compiler, Android Studio's dexer and/or Proguard which
     * forces us to use this useless wrapper class, as otherwise an error pops up, that we try to
     * assign an Int to an Object O.o
     */
    class TotalEpisodesResult(val value: Int)
}