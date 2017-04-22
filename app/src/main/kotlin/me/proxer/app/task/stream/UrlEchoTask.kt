package me.proxer.app.task.stream

import android.support.v7.app.AppCompatActivity
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri

/**
 * @author Ruben Gees
 */
class UrlEchoTask(
        private val contentType: String = "text/html",
        private val notFoundAction: ((AppCompatActivity) -> Unit)? = null
) : WorkerTask<String, StreamResolutionResult>() {

    override fun work(input: String) = when (notFoundAction) {
        null -> StreamResolutionResult(Utils.parseAndFixUrl(input).androidUri(), contentType)
        else -> StreamResolutionResult(Utils.parseAndFixUrl(input).androidUri(), contentType, notFoundAction)
    }
}
