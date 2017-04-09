package me.proxer.app.task.stream.factory

import android.content.Intent
import android.net.Uri
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.dialog.AppRequiredDialog
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult

/**
 * @author Ruben Gees
 */
class CrunchyrollResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Crunchyroll"
    override fun create() = TaskBuilder.task(CrunchyrollTask()).build()

    class CrunchyrollTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("media_id=(\\d+)")
        }

        override fun work(input: String): StreamResolutionResult {
            val id = regex.find(input)?.groupValues?.get(1) ?: throw StreamResolutionException()

            return StreamResolutionResult(Intent(Intent.ACTION_VIEW, Uri.parse("crunchyroll://media/$id")), {
                AppRequiredDialog.show(it, "Crunchyroll", "com.crunchyroll.crunchyroid")
            })
        }
    }
}
