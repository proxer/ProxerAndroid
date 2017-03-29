package me.proxer.app.util.extension

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.content.res.AppCompatResources
import me.proxer.app.R
import me.proxer.library.entitiy.info.EntrySeasonInfo
import me.proxer.library.enums.*

fun Medium.toAppString(context: Context): String? {
    return context.getString(when (this) {
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

fun MediaLanguage.toGeneralLanguage(): Language {
    return when (this) {
        MediaLanguage.GERMAN, MediaLanguage.GERMAN_SUB, MediaLanguage.GERMAN_DUB -> Language.GERMAN
        MediaLanguage.ENGLISH, MediaLanguage.ENGLISH_SUB, MediaLanguage.ENGLISH_DUB -> Language.ENGLISH
    }
}

fun Medium.toCategory(): Category {
    return when (this) {
        Medium.ANIMESERIES, Medium.MOVIE, Medium.OVA, Medium.HENTAI -> Category.ANIME
        Medium.MANGASERIES, Medium.ONESHOT, Medium.DOUJIN, Medium.HMANGA -> Category.MANGA
    }
}

fun EntrySeasonInfo.toAppStringStart(context: Context): String {
    return when (season) {
        Season.WINTER -> context.getString(R.string.season_winter_start, year)
        Season.SPRING -> context.getString(R.string.season_spring_start, year)
        Season.SUMMER -> context.getString(R.string.season_summer_start, year)
        Season.AUTUMN -> context.getString(R.string.season_autumn_start, year)
        Season.UNSPECIFIED -> year.toString()
        Season.UNSPECIFIED_ALT -> year.toString()
    }
}

fun EntrySeasonInfo.toAppStringEnd(context: Context): String {
    return when (season) {
        Season.WINTER -> context.getString(R.string.season_winter_end, year)
        Season.SPRING -> context.getString(R.string.season_spring_end, year)
        Season.SUMMER -> context.getString(R.string.season_summer_end, year)
        Season.AUTUMN -> context.getString(R.string.season_autumn_end, year)
        Season.UNSPECIFIED -> year.toString()
        Season.UNSPECIFIED_ALT -> year.toString()
    }
}

fun MediaState.toAppString(context: Context): String {
    return context.getString(when (this) {
        MediaState.PRE_AIRING -> R.string.media_state_pre_airing
        MediaState.AIRING -> R.string.media_state_airing
        MediaState.CANCELLED -> R.string.media_state_cancelled
        MediaState.CANCELLED_SUB -> R.string.media_state_cancelled_sub
        MediaState.FINISHED -> R.string.media_state_finished
    })
}

fun License.toAppString(context: Context): String {
    return context.getString(when (this) {
        License.LICENSED -> R.string.license_licensed
        License.NOT_LICENSED -> R.string.license_not_licensed
        License.UNKNOWN -> R.string.license_unknown
    })
}

fun FskConstraint.toAppStringDescription(context: Context): String {
    return context.getString(when (this) {
        FskConstraint.FSK_0 -> R.string.fsk_0_description
        FskConstraint.FSK_6 -> R.string.fsk_6_description
        FskConstraint.FSK_12 -> R.string.fsk_12_description
        FskConstraint.FSK_16 -> R.string.fsk_16_description
        FskConstraint.FSK_18 -> R.string.fsk_18_description
        FskConstraint.BAD_LANGUAGE -> R.string.fsk_bad_language_description
        FskConstraint.FEAR -> R.string.fsk_fear_description
        FskConstraint.VIOLENCE -> R.string.fsk_violence_description
        FskConstraint.SEX -> R.string.fsk_sex_description
    })
}

fun FskConstraint.toAppDrawable(context: Context): Drawable {
    return AppCompatResources.getDrawable(context, when (this) {
        FskConstraint.FSK_0 -> R.drawable.ic_fsk_0
        FskConstraint.FSK_6 -> R.drawable.ic_fsk_6
        FskConstraint.FSK_12 -> R.drawable.ic_fsk_12
        FskConstraint.FSK_16 -> R.drawable.ic_fsk_16
        FskConstraint.FSK_18 -> R.drawable.ic_fsk_18
        FskConstraint.BAD_LANGUAGE -> R.drawable.ic_fsk_bad_language
        FskConstraint.FEAR -> R.drawable.ic_fsk_fear
        FskConstraint.SEX -> R.drawable.ic_fsk_sex
        FskConstraint.VIOLENCE -> R.drawable.ic_fsk_violence
    }) ?: throw NullPointerException("Could not resolve Drawable for fsk constraint: $this")
}