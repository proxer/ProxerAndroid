package com.proxerme.app.stream.resolver

import android.net.Uri
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.HttpUrl

/**
 * Resolver for the Daisuki hoster. Currently it only redirects to the homepage as the app does not
 * offer support for sending Intents and the mobile website is not usable without the app.
 *
 * @author Ruben Gees
 */
class DaisukiResolver : StreamResolver() {

    override val name = "daisuki.net"

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        return StreamResolutionResult(Uri.parse("http://daisuki.net"), "text/html")
    }
}