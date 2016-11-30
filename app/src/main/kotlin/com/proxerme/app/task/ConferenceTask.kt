package com.proxerme.app.task

import android.content.Context
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import org.jetbrains.anko.doAsync
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferenceTask(private val contextCallback: () -> Context) :
        BaseTask<List<LocalConference>>() {

    override val isWorking: Boolean
        get() = !(future?.isDone ?: true)

    private var future: Future<Unit>? = null

    override fun execute(successCallback: (List<LocalConference>) -> Unit,
                         exceptionCallback: (Exception) -> Unit) {
        start {
            future = doAsync {
                try {
                    finishSuccessful(contextCallback.invoke().chatDatabase.getConferences(),
                            successCallback)
                } catch (exception: Exception) {
                    finishWithException(exception, exceptionCallback)
                }
            }
        }
    }

    override fun cancel() {
        future?.cancel(true)
        future = null
    }

    override fun reset() {
        cancel()
    }
}