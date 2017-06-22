package me.proxer.app.util.data

/**
 * @author Ruben Gees
 */
class NonPersistentCookieJar : okhttp3.CookieJar {

    private val cookieStore = LinkedHashSet<okhttp3.Cookie>()

    /**
     * We actually don't use cookies, only the myvi stream resolver requires them.
     */
    @Synchronized
    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        cookieStore.addAll(cookies.filter {
            it.domain() == "myvi.ru"
        })
    }

    @Synchronized
    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        val matchingCookies = ArrayList<okhttp3.Cookie>()

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
