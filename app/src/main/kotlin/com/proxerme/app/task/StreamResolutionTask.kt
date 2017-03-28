package com.proxerme.app.task

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import com.proxerme.app.R
import com.proxerme.app.fragment.anime.AnimeFragment.StreamResolverInput
import com.proxerme.app.stream.StreamResolverFactory
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.task.framework.BaseTask
import com.proxerme.app.util.Utils
import com.proxerme.app.util.extension.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class StreamResolutionTask(successCallback: ((StreamResolutionResult) -> Unit)? = null,
                           exceptionCallback: ((Exception) -> Unit)? = null) :
        BaseTask<Pair<StreamResolverInput, String>, StreamResolutionResult>(successCallback,
                exceptionCallback) {

    override val isWorking: Boolean
        get() = !(future?.isDone ?: true)

    private val handler = Handler(Looper.getMainLooper())
    private var future: Future<Unit>? = null

    override fun execute(input: Pair<StreamResolverInput, String>) {
        start {
            future = doAsync {
                try {
                    val result = StreamResolverFactory.getResolverFor(input.first.name)
                            ?.resolve(Utils.parseAndFixUrl(input.second)) ?: throw NoResolverException()

                    handler.post {
                        cancel()

                        finishSuccessful(result)
                    }
                } catch (exception: Exception) {
                    handler.post {
                        cancel()

                        finishWithException(exception)
                    }
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

    override fun destroy() {
        reset()

        super.destroy()
    }

    class StreamResolutionResult {

        companion object {
            const val ACTION_SHOW_MESSAGE = "android.intent.action.SHOW_MESSAGE"
            const val MESSAGE = "extra_message"

            private val defaultNotFoundAction: (AppCompatActivity) -> Unit = {
                it.snackbar(it.find(android.R.id.content), R.string.error_activity_not_found)
            }
        }

        val intent: Intent
        val notFoundAction: (AppCompatActivity) -> Unit

        constructor(intent: Intent,
                    notFoundAction: (AppCompatActivity) -> Unit = defaultNotFoundAction) {
            this.intent = intent
            this.notFoundAction = notFoundAction
        }

        constructor(uri: Uri, mimeType: String,
                    notFoundAction: (AppCompatActivity) -> Unit = defaultNotFoundAction) {
            this.intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mimeType) }
            this.notFoundAction = notFoundAction
        }

        constructor(message: String) {
            this.intent = Intent(ACTION_SHOW_MESSAGE).apply { putExtra(MESSAGE, message) }
            this.notFoundAction = defaultNotFoundAction
        }
    }

    class NoResolverException : Exception()
    class StreamResolutionException : Exception()
}
