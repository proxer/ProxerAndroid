package me.proxer.app.manga

import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class MangaLinkException(val chapterTitle: String, val link: HttpUrl) : Exception()
