package me.proxer.app.util

import android.content.Context
import com.proxerme.library.enums.Language
import com.proxerme.library.enums.MediaLanguage
import com.proxerme.library.enums.Medium
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
object EnumMapper {

    fun mediumToString(context: Context, medium: Medium): String {
        return context.getString(when (medium) {
            Medium.ANIMESERIES -> R.string.medium_anime_series
            Medium.HENTAI -> R.string.medium_hentai
            Medium.MOVIE -> R.string.medium_movie
            Medium.OVA -> R.string.medium_ova
            Medium.MANGASERIES -> R.string.medium_manga_series
            Medium.DOUJIN -> R.string.medium_doujin
            Medium.HMANGA -> R.string.medium_h_manga
            Medium.ONESHOT -> R.string.medium_oneshot
        })
    }

    fun mediaLanguageToGeneralLanguage(language: MediaLanguage): Language {
        return when (language) {
            MediaLanguage.GERMAN, MediaLanguage.GERMAN_SUB, MediaLanguage.GERMAN_DUB -> Language.GERMAN
            MediaLanguage.ENGLISH, MediaLanguage.ENGLISH_SUB, MediaLanguage.ENGLISH_DUB -> Language.ENGLISH
        }
    }
}
