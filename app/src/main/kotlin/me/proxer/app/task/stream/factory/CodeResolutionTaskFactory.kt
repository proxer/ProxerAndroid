package me.proxer.app.task.stream.factory

import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult

/**
 * @author Ruben Gees
 */
class CodeResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Code"
    override val supports = { name: String -> name.contains(this.name, false) || name.contains("Nachricht", false) }
    override fun create() = TaskBuilder.task(CodeTask()).build()

    class CodeTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("</?h1>")
        }

        override fun work(input: String) = StreamResolutionResult(input.replace(regex, ""))
    }
}
