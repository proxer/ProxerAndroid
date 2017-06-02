package me.proxer.app.task.stream.factory

import com.rubengees.ktask.base.LeafTask
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.application.MainApplication
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.extension.androidUri
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.lang.Exception
import java.util.regex.Pattern

/**
 * @author Ruben Gees
 */
class Mp4UploadResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "MP4Upload"
    override fun create() = TaskBuilder.task(Mp4UploadTask())
            .build()

    class Mp4UploadTask : LeafTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("-(.*?)${Pattern.quote(".")}", RegexOption.DOT_MATCHES_ALL)
        }

        override val isWorking: Boolean
            get() = call != null

        private var call: Call? = null

        override fun execute(input: String) {
            start {
                val regexResult = regex.find(input) ?: throw StreamResolutionException()
                val id = regexResult.groupValues[1]

                if (id.isBlank()) {
                    throw StreamResolutionException()
                }

                call = MainApplication.client.newCall(Request.Builder()
                        .post(FormBody.Builder().add("op", "download2").add("id", id).build())
                        .url(HttpUrl.parse("https://mp4upload.com/$id"))
                        .build())

                try {
                    val response = call?.execute() ?: throw NullPointerException("call cannot be null")
                    val url = response.networkResponse()?.request()?.url()

                    internalCancel()

                    if (response.isSuccessful && url != null) {
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
