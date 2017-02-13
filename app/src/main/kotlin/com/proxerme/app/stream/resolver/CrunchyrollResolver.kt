package com.proxerme.app.stream.resolver

import android.content.Intent
import android.net.Uri
import com.proxerme.app.dialog.AppRequiredDialog
import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionException
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CrunchyrollResolver : StreamResolver() {

    override val name = "Crunchyroll"

    private val regex = Regex("media_id=(\\d+)")

    override fun resolve(url: String): StreamResolutionResult {
        val id = regex.find(url)?.groupValues?.get(1)
                ?: throw StreamResolutionException()

        return StreamResolutionResult(Intent(Intent.ACTION_VIEW,
                Uri.parse("crunchyroll://media/$id")), {
            AppRequiredDialog.show(it, "Crunchyroll", "com.crunchyroll.crunchyroid")
        })
    }
}