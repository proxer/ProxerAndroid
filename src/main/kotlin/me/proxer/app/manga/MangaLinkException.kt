package me.proxer.app.manga

import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class MangaLinkException(val link: HttpUrl) : Exception()
