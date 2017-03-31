package me.proxer.app.entity

import me.proxer.library.entitiy.info.AnimeEpisode
import me.proxer.library.entitiy.info.Episode
import me.proxer.library.entitiy.info.MangaEpisode
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class EpisodeRow(val category: Category, val userProgress: Int, val totalEpisodes: Int, episodes: List<Episode>) {

    val number: Int
    val title: String?
    val languageHosterList: List<Pair<MediaLanguage, List<String>?>>

    init {
        if (episodes.isEmpty()) {
            throw IllegalArgumentException("At least one episode has to be passed.")
        }

        this.number = episodes.first().number

        if (episodes.first() is MangaEpisode) {
            this.title = (episodes.first() as MangaEpisode).title
            this.languageHosterList = episodes.map { it.language to null }
        } else if (episodes.first() is AnimeEpisode) {
            this.title = null
            this.languageHosterList = episodes.map { it.language to (it as AnimeEpisode).hosterImages }
        } else {
            throw IllegalArgumentException("Unknown type: ${episodes.first().javaClass}")
        }
    }
}
