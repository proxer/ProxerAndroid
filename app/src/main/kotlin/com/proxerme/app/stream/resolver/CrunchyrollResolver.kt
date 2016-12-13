package com.proxerme.app.stream.resolver

import android.content.Intent
import android.net.Uri
import com.proxerme.app.dialog.CrunchyrollDialog
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionException
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CrunchyrollResolver : StreamResolver() {

    override val name = "crunchyroll.com"

    private val regex = Regex("media_id=(\\d+)")

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        val id = regex.find(url.toString())?.groupValues?.get(1)
                ?: throw StreamResolutionException()

        return StreamResolutionResult(Intent(Intent.ACTION_VIEW,
                Uri.parse("crunchyroll://media/$id")), { CrunchyrollDialog.show(it) })
    }
}