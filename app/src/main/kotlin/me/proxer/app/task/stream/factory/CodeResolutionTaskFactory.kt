package me.proxer.app.task.stream.factory

import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.compat.HtmlCompat

/**
 * @author Ruben Gees
 */
class CodeResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Code"
    override val supports = { name: String -> name.contains(this.name, true) || name.contains("Nachricht", true) }
    override fun create() = TaskBuilder.task(CodeTask()).build()

    class CodeTask : WorkerTask<String, StreamResolutionResult>() {
        override fun work(input: String) = StreamResolutionResult(HtmlCompat.fromHtml(input).trim())
    }
}
