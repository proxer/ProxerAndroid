package me.proxer.app.task.stream

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import com.rubengees.ktask.base.BaseTask
import com.rubengees.ktask.base.Task
import me.proxer.app.R
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionInput
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.task.stream.factory.*
import me.proxer.app.util.extension.snackbar
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class StreamResolutionTask : BaseTask<StreamResolutionInput, StreamResolutionResult>() {

    companion object {
        private val resolutionTaskFactories = arrayOf(AkibaPassResolutionTaskFactory(),
                AmazonPrimeVideoResolutionTaskFactory(), AnimeOnDemandResolutionTaskFactory(),
                AuravidResolutionTaskFactory(), ClipfishResolutionTaskFactory(), CodeResolutionTaskFactory(),
                CrunchyrollResolutionTaskFactory(), DailymotionResolutionTaskFactory(), DaisukiResolutionTaskFactory(),
                Mp4UploadResolutionTaskFactory(), ProsiebenMAXXResolutionTaskFactory(),
                ProxerStreamResolutionTaskFactory(), StreamcloudResolutionTaskFactory(),
                VideoWeedResolutionTaskFactory(), ViewsterResolutionTaskFactory(), YourUploadResolutionTaskFactory(),
                YouTubeResolutionTaskFactory())

        fun isSupported(name: String) = resolutionTaskFactories.any { it.supports(name) }
    }

    override val isWorking
        get() = tasks.any { it.task.isWorking }

    private val tasks: List<StreamResolutionTaskWrapper>

    init {
        tasks = resolutionTaskFactories.map {
            StreamResolutionTaskWrapper(it.supports, it.create())
        }

        initCallbacks()
    }

    override fun execute(input: StreamResolutionInput) {
        start {
            tasks.find { it.appliesTo(input.name) }?.task?.execute(input.data)
                    ?: finishWithError(NoResolverException())
        }
    }

    override fun onInnerStart(callback: () -> Unit): StreamResolutionTask {
        return this.apply { tasks.forEach { it.task.onInnerStart(callback) } }
    }

    override fun cancel() {
        super.cancel()

        tasks.forEach { it.task.cancel() }
    }

    override fun reset() {
        super.reset()

        tasks.forEach { it.task.reset() }
    }

    override fun destroy() {
        super.destroy()

        tasks.forEach { it.task.destroy() }
    }

    override fun retainingDestroy() {
        super.retainingDestroy()

        tasks.forEach { it.task.retainingDestroy() }
    }

    override fun restoreCallbacks(from: Task<StreamResolutionInput, StreamResolutionResult>) {
        super.restoreCallbacks(from)

        if (from !is StreamResolutionTask) {
            throw IllegalArgumentException("The passed task must have the same type.")
        }

        tasks.forEachIndexed { index, it -> it.task.restoreCallbacks(from.tasks[index].task) }

        initCallbacks()
    }

    private fun initCallbacks() {
        tasks.forEach {
            it.task.onSuccess {
                finishSuccessful(it)
            }

            it.task.onError {
                finishWithError(it)
            }
        }
    }

    private class StreamResolutionTaskWrapper(val appliesTo: (String) -> Boolean,
                                              val task: Task<String, StreamResolutionResult>)

    class StreamResolutionInput(val name: String, val data: String)
    class StreamResolutionResult(val intent: Intent,
                                 val notFoundAction: (AppCompatActivity) -> Unit = defaultNotFoundAction) {

        companion object {
            const val ACTION_SHOW_MESSAGE = "me.proxer.app.intent.action.SHOW_MESSAGE"
            const val MESSAGE_EXTRA = "message"

            private val defaultNotFoundAction: (AppCompatActivity) -> Unit = {
                it.snackbar(it.find<ViewGroup>(android.R.id.content), R.string.error_activity_not_found)
            }
        }

        constructor(uri: Uri, mimeType: String, notFoundAction: (AppCompatActivity) -> Unit = defaultNotFoundAction) :
                this(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mimeType) }, notFoundAction)

        constructor(message: CharSequence) : this(Intent(ACTION_SHOW_MESSAGE).apply {
            putExtra(MESSAGE_EXTRA, message)
        })
    }

    class NoResolverException : Exception()
    class StreamResolutionException : Exception()
}
