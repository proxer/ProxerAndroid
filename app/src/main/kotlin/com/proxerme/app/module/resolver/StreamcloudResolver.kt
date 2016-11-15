package com.proxerme.app.module.resolver

import com.proxerme.app.application.MainApplication
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class StreamcloudResolver : StreamResolver() {

    override val name = "streamcloud"

    private val fromRegex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")
    private val fileRegex = Regex("file: \"(.+?)\",")

    override fun resolve(url: String): ResolverResult {
        var response = MainApplication.proxerConnection.httpClient.newCall(Request.Builder()
                .get()
                .url(url)
                .build()).execute()
        val formValues = FormBody.Builder()

        for (i in fromRegex.findAll(validateAndGetResult(response))) {
            formValues.add(i.groupValues[1], i.groupValues[2].replace("download1", "download2"))
        }

        response = MainApplication.proxerConnection.httpClient.newCall(Request.Builder()
                .post(formValues.build())
                .url(url)
                .build())
                .execute()

        val result = fileRegex.find(validateAndGetResult(response))?.groupValues?.get(1)
                ?: throw IOException()

        return ResolverResult(result, "video/mp4")
    }
}