package me.proxer.app.util

import android.os.Build
import android.text.Html
import android.text.Spanned

/**
 * @author Ruben Gees
 */
object HtmlCompat {
    fun fromHtml(source: String): Spanned {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            return Html.fromHtml(source)
        }
    }
}
