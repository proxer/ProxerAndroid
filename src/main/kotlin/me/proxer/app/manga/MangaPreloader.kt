package me.proxer.app.manga

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import me.proxer.app.GlideRequests
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.wrapper.OriginalSizeGlideTarget
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.util.ProxerUrls
import timber.log.Timber
import java.io.File

/**
 * @author Ruben Gees
 */
class MangaPreloader {

    var glide: GlideRequests? = null

    private val preloadTargets = mutableListOf<Target<File>>()

    fun preload(chapter: Chapter) {
        preloadTargets.forEach { glide?.clear(it) }
        preloadTargets.clear()

        val preloadList = chapter.pages
            ?.map { ProxerUrls.mangaPageImage(chapter.server, chapter.entryId, chapter.id, it.decodedName).toString() }
            ?: emptyList()

        if (preloadList.isNotEmpty()) {
            val preloadMap = preloadList
                .asSequence()
                .mapIndexed { index, url -> url to preloadList.getOrNull(index + 1) }
                .associate { it }

            recursivePreload(preloadMap, preloadList.first())
        }
    }

    fun cancel() {
        preloadTargets.forEach { glide?.clear(it) }
        preloadTargets.clear()
    }

    private fun recursivePreload(links: Map<String, String?>, next: String, failures: Int = 0) {
        Timber.d("Preloading $next")

        val target = GlidePreloadTarget(links, next, failures)

        preloadTargets += target

        glide
            ?.downloadOnly()
            ?.load(next)
            ?.logErrors()
            ?.into(target)
    }

    internal inner class GlidePreloadTarget(
        private val links: Map<String, String?>,
        private val next: String,
        private val failures: Int
    ) : OriginalSizeGlideTarget<File>() {

        override fun onResourceReady(resource: File, transition: Transition<in File>?) {
            val afterNext = links[next]

            if (afterNext != null) {
                recursivePreload(links, afterNext)
            }
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            if (failures <= 2) {
                recursivePreload(links, next, failures + 1)
            }
        }
    }
}
