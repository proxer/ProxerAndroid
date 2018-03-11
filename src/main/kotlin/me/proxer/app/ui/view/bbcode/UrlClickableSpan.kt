package me.proxer.app.ui.view.bbcode

import android.content.Context
import android.content.ContextWrapper
import android.text.style.ClickableSpan
import android.view.View
import me.proxer.app.base.BaseActivity
import okhttp3.HttpUrl

internal class UrlClickableSpan(private val url: HttpUrl) : ClickableSpan() {

    override fun onClick(widget: View) {
        findBaseActivity(widget.context)?.showPage(url)
    }

    private fun findBaseActivity(currentContext: Context): BaseActivity? = when (currentContext) {
        is BaseActivity -> currentContext
        is ContextWrapper -> findBaseActivity(currentContext.baseContext)
        else -> null
    }
}
