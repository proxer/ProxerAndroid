package me.proxer.app.ui.view.bbcode

import android.text.style.ClickableSpan
import android.view.View
import me.proxer.app.base.BaseActivity
import okhttp3.HttpUrl

internal class UrlClickableSpan(private val url: HttpUrl) : ClickableSpan() {

    override fun onClick(widget: View) {
        (widget.context as? BaseActivity)?.showPage(url)
    }
}
