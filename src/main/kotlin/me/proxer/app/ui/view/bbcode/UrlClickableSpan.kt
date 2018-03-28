package me.proxer.app.ui.view.bbcode

import android.text.style.ClickableSpan
import android.view.View
import okhttp3.HttpUrl

internal class UrlClickableSpan(private val url: HttpUrl) : ClickableSpan() {

    override fun onClick(widget: View) {
        BBUtils.findBaseActivity(widget.context)?.showPage(url)
    }
}
