package me.proxer.app.task.stream

import com.rubengees.ktask.base.LeafTask
import me.proxer.app.application.MainApplication
import me.proxer.app.util.Utils
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.lang.Exception

/**
 * @author Ruben Gees
 */
class LinkResolutionTask<I>(private val userAgent: String? = null,
                            private val urlTransformation: (I) -> HttpUrl = {
                                Utils.parseAndFixUrl(it.toString())
                            }) : LeafTask<I, String>() {

    override val isWorking: Boolean
        get() = call != null

    private var call: Call? = null

    override fun execute(input: I) {
        start {
            val url = urlTransformation.invoke(input)

            call = MainApplication.client.newCall(Request.Builder()
                    .apply {
                        if (userAgent != null) {
                            addHeader("User-Agent", userAgent)
                        }
                    }
                    .get()
                    .url(url)
                    .build())

            try {
                val response = call?.execute() ?: throw NullPointerException("call cannot be null")
                val body = response.body()
                val content = body.string()

                internalCancel()
                body.close()

                if (response.isSuccessful) {
                    finishSuccessful(content)
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