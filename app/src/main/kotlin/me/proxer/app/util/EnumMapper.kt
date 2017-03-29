package me.proxer.app.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.content.res.AppCompatResources
import me.proxer.app.R
import me.proxer.library.entitiy.info.EntrySeasonInfo
import me.proxer.library.enums.*

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

    fun mediumToCategory(medium: Medium): Category {
        return when (medium) {
            Medium.ANIMESERIES, Medium.MOVIE, Medium.OVA, Medium.HENTAI -> Category.ANIME
            Medium.MANGASERIES, Medium.ONESHOT, Medium.DOUJIN, Medium.HMANGA -> Category.MANGA
        }
    }

    fun seasonStartToString(context: Context, seasonInfo: EntrySeasonInfo): String {
        return when (seasonInfo.season) {
            Season.WINTER -> context.getString(R.string.season_winter_start, seasonInfo.year)
            Season.SPRING -> context.getString(R.string.season_spring_start, seasonInfo.year)
            Season.SUMMER -> context.getString(R.string.season_summer_start, seasonInfo.year)
            Season.AUTUMN -> context.getString(R.string.season_autumn_start, seasonInfo.year)
            Season.UNSPECIFIED -> seasonInfo.year.toString()
            Season.UNSPECIFIED_ALT -> seasonInfo.year.toString()
        }
    }

    fun seasonEndToString(context: Context, seasonInfo: EntrySeasonInfo): String {
        return when (seasonInfo.season) {
            Season.WINTER -> context.getString(R.string.season_winter_end, seasonInfo.year)
            Season.SPRING -> context.getString(R.string.season_spring_end, seasonInfo.year)
            Season.SUMMER -> context.getString(R.string.season_summer_end, seasonInfo.year)
            Season.AUTUMN -> context.getString(R.string.season_autumn_end, seasonInfo.year)
            Season.UNSPECIFIED -> seasonInfo.year.toString()
            Season.UNSPECIFIED_ALT -> seasonInfo.year.toString()
        }
    }

    fun mediaStateToString(context: Context, state: MediaState): String {
        return context.getString(when (state) {
            MediaState.PRE_AIRING -> R.string.media_state_pre_airing
            MediaState.AIRING -> R.string.media_state_airing
            MediaState.CANCELLED -> R.string.media_state_cancelled
            MediaState.CANCELLED_SUB -> R.string.media_state_cancelled_sub
            MediaState.FINISHED -> R.string.media_state_finished
        })
    }

    fun licenseToString(context: Context, licence: License): String {
        return context.getString(when (licence) {
            License.LICENSED -> R.string.license_licensed
            License.NOT_LICENSED -> R.string.license_not_licensed
            License.UNKNOWN -> R.string.license_unknown
        })
    }

    fun fskConstraintToString(context: Context, fskConstraint: FskConstraint): String {
        return context.getString(when (fskConstraint) {
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

    fun fskConstraintToDrawable(context: Context, fskConstraint: FskConstraint): Drawable {
        return AppCompatResources.getDrawable(context, when (fskConstraint) {
            FskConstraint.FSK_0 -> R.drawable.ic_fsk_0
            FskConstraint.FSK_6 -> R.drawable.ic_fsk_6
            FskConstraint.FSK_12 -> R.drawable.ic_fsk_12
            FskConstraint.FSK_16 -> R.drawable.ic_fsk_16
            FskConstraint.FSK_18 -> R.drawable.ic_fsk_18
            FskConstraint.BAD_LANGUAGE -> R.drawable.ic_fsk_bad_language
            FskConstraint.FEAR -> R.drawable.ic_fsk_fear
            FskConstraint.SEX -> R.drawable.ic_fsk_sex
            FskConstraint.VIOLENCE -> R.drawable.ic_fsk_violence
        }) ?: throw NullPointerException("Could not resolve Drawable for fsk constraint: $fskConstraint")
    }
}
