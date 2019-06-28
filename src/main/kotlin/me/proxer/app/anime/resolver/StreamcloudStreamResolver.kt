package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object StreamcloudStreamResolver : StreamResolver() {

    private val formRegex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")
    private val fileRegex = Regex("file: \"(.+?)\",")

    override val name = "Streamcloud"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime.link(id)
        .buildSingle()
        .flatMap { url ->
            client
                .newCall(
                    Request.Builder()
                        .get()
                        .url(url)
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .header("Connection", "close")
                        .build()
                )
                .toBodySingle()
                .map {
                    val formValues = FormBody.Builder()
                        .apply {
                            for (i in formRegex.findAll(it)) {
                                add(i.groupValues[1], i.groupValues[2].replace("download1", "download2"))
                            }
                        }
                        .build()

                    if (formValues.size == 0) {
                        throw StreamResolutionException()
                    }

                    formValues
                }
                .flatMap {
                    client
                        .newCall(
                            Request.Builder()
                                .post(it)
                                .url(url)
                                .header("User-Agent", GENERIC_USER_AGENT)
                                .header("Connection", "close")
                                .build()
                        )
                        .toBodySingle()
                }
                .map {
                    fileRegex.find(it)?.groupValues?.get(1)?.toHttpUrlOrNull()
                        ?: throw StreamResolutionException()
                }
                .map { StreamResolutionResult.Video(it, "video/mp4", url, internalPlayerOnly = true) }
        }
}
