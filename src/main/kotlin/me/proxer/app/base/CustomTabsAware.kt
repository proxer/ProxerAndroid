package me.proxer.app.base

import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
interface CustomTabsAware {

    fun setLikelyUrl(url: HttpUrl): Boolean

    fun showPage(url: HttpUrl, forceBrowser: Boolean = false, skipCheck: Boolean = false)
}
