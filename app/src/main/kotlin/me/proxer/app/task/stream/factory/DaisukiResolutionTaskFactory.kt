package me.proxer.app.task.stream.factory

import android.net.Uri
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult

/**
 * @author Ruben Gees
 */
class DaisukiResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Daisuki"
    override fun create() = TaskBuilder.task(DaisukiTask()).build()

    class DaisukiTask : WorkerTask<String, StreamResolutionResult>() {
        override fun work(input: String) = StreamResolutionResult(Uri.parse("https://daisuki.net"), "text/html")
    }
}
