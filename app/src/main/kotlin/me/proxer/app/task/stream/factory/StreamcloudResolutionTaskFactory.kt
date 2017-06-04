package me.proxer.app.task.stream.factory

import android.net.Uri
import com.rubengees.ktask.base.LeafTask
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.application.MainApplication
import me.proxer.app.task.stream.LinkResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionException
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

/**
 * @author Ruben Gees
 */
class StreamcloudResolutionTaskFactory : HosterResolutionTaskFactory() {

    override val name = "Streamcloud"
    override fun create() = TaskBuilder.task(LinkResolutionTask<String>())
            .then(StreamcloudTask())
            .inputEcho()
            .then(StreamcloudChallengeTask())
            .then(StreamcloudAfterChallengeTask())
            .build()

    class StreamcloudTask : WorkerTask<String, FormBody>() {

        private companion object {
            private val regex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")
        }

        override fun work(input: String): FormBody {
            val formValues = FormBody.Builder().apply {
                for (i in regex.findAll(input)) {
                    if (i.groupValues.size < 2) {
                        throw StreamResolutionException()
                    }

                    add(i.groupValues[1], i.groupValues[2].replace("download1", "download2"))
                }
            }.build()

            if (formValues.size() == 0) {
                throw StreamResolutionException()
            }

            return formValues
        }
    }

    class StreamcloudChallengeTask : LeafTask<Pair<String, FormBody>, String>() {

        override val isWorking: Boolean
            get() = call != null

        private var call: Call? = null

        override fun execute(input: Pair<String, FormBody>) {
            start {
                call = MainApplication.client.newCall(Request.Builder()
                        .post(input.second)
                        .url(input.first)
                        .build())

                try {
                    val response = call?.execute() ?: throw NullPointerException("call cannot be null")
                    val body = response.body()
                    val content = body?.string()

                    internalCancel()
                    body?.close()

                    if (response.isSuccessful && content != null) {
                        finishSuccessful(content)
                    } else {
                        finishWithError(IOException())
                    }
                } catch (error: Throwable) {
                    internalCancel()

                    finishWithError(error)
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

    class StreamcloudAfterChallengeTask : WorkerTask<String, StreamResolutionResult>() {

        private companion object {
            private val regex = Regex("file: \"(.+?)\",")
        }

        override fun work(input: String): StreamResolutionResult {
            val result = Uri.parse(regex.find(input)?.groupValues?.get(1) ?: throw StreamResolutionException())

            return StreamResolutionResult(result, "video/mp4")
        }
    }
}
