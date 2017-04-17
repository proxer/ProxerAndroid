package me.proxer.app.task.stream.factory

import com.rubengees.ktask.base.LeafTask
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.extension.androidUri
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.lang.Exception

/**
 * @author Ruben Gees
 */
class YourUploadResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "YourUpload"
    override fun create() = TaskBuilder.task(LinkResolutionTask<String>())
            .then(YourUploadTask())
            .inputEcho()
            .then(YourUploadApiTask())
            .build()

    class YourUploadTask : WorkerTask<String, String>() {

        private companion object {
            private val regex = Regex("file: '(.*?)'")
        }

        override fun work(input: String): String {
            val regexResult = regex.find(input) ?: throw StreamResolutionException()
            val file = regexResult.groupValues[1]

            if (file.isBlank()) {
                throw StreamResolutionException()
            }

            return file
        }
    }

    class YourUploadApiTask : LeafTask<Pair<String, String>, StreamResolutionResult>() {

        override val isWorking: Boolean
            get() = call != null

        private var call: Call? = null

        override fun execute(input: Pair<String, String>) {
            start {
                call = MainApplication.client.newCall(Request.Builder()
                        .header("Referer", input.first)
                        .head()
                        .url(HttpUrl.parse("http://yourupload.com${input.second}"))
                        .build())

                try {
                    val response = call?.execute() ?: throw NullPointerException("call cannot be null")
                    val url = response.networkResponse().request().url()

                    internalCancel()

                    if (response.isSuccessful) {
                        finishSuccessful(StreamResolutionResult(url.androidUri(), "video/mp4"))
                    } else {
                        finishWithError(IOException())
                    }
                } catch (error: Throwable) {
                    internalCancel()

                    finishWithError(error as Exception)
                }
            }
        }

        override fun cancel() {
            super.cancel()

            internalCancel()
        }

        private fun internalCancel() {
            call?.cancel()
            call = null
        }
    }
}
