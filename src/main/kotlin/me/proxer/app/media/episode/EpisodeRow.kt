package me.proxer.app.media.episode

import me.proxer.library.entity.info.AnimeEpisode
import me.proxer.library.entity.info.Episode
import me.proxer.library.entity.info.MangaEpisode
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage

/**
 * @author Ruben Gees
 */
class EpisodeRow(val category: Category, val userProgress: Int, val episodeAmount: Int, episodes: List<Episode>) {

    val number: Int
    val title: String?
    val languageHosterList: List<Pair<MediaLanguage, List<String>?>>

    init {
        val firstEpisode = episodes.firstOrNull()

        if (firstEpisode != null) {
            title = (firstEpisode as? MangaEpisode)?.title
            number = firstEpisode.number
        } else {
            title = null
            number = -1
        }

        languageHosterList = episodes.map {
            when (it) {
                is AnimeEpisode -> it.language to it.hosterImages
                else -> it.language to null
            }
        }
    }
}
