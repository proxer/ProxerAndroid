package me.proxer.app.anime.resolver

import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.FormBody
import okhttp3.Request

/**
 * @author Ruben Gees
 */
class StreamcloudStreamResolver : StreamResolver {

    private companion object {
        private val formRegex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")
        private val fileRegex = Regex("file: \"(.+?)\",")
    }

    override val name = "Streamcloud"
    override val internalPlayerOnly = true

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
        .buildSingle()
        .flatMap { url ->
            client.newCall(Request.Builder()
                .get()
                .url(url)
                .header("User-Agent", GENERIC_USER_AGENT)
                .build())
                .toBodySingle()
                .map {
                    val formValues = FormBody.Builder().apply {
                        for (i in formRegex.findAll(it)) {
                            if (i.groupValues.size < 2) {
                                throw StreamResolutionException()
                            }

                            add(i.groupValues[1], i.groupValues[2].replace("download1", "download2"))
                        }
                    }.build()

                    if (formValues.size() == 0) {
                        throw StreamResolutionException()
                    }

                    formValues
                }
                .flatMap {
                    client.newCall(Request.Builder()
                        .post(it)
                        .url(url)
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .build())
                        .toBodySingle()
                }
                .map {
                    val result = Uri.parse(fileRegex.find(it)?.groupValues?.get(1)
                        ?: throw StreamResolutionException())

                    StreamResolutionResult(result, "video/mp4", url)
                }
        }
}
