package me.proxer.app.util

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class NonPersistentCookieJar : CookieJar {

    private val cookieStore = LinkedHashSet<Cookie>()

    /**
     * We actually don't use cookies, only the myvi stream resolver requires them.
     */
    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.addAll(cookies.filter {
            it.domain() == "myvi.ru"
        })
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val matchingCookies = ArrayList<Cookie>()

        cookieStore.iterator().let {
            while (it.hasNext()) {
                val cookie = it.next()

                when {
                    cookie.expiresAt() < System.currentTimeMillis() -> it.remove()
                    cookie.matches(url) -> matchingCookies.add(cookie)
                }
            }
        }

        return matchingCookies
    }
}
