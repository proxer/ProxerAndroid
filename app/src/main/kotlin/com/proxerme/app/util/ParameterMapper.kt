package com.proxerme.app.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.content.res.AppCompatResources
import com.proxerme.app.R
import com.proxerme.library.connection.info.entity.EntrySeason
import com.proxerme.library.parameters.*
import com.proxerme.library.parameters.CategoryParameter.ANIME
import com.proxerme.library.parameters.CategoryParameter.MANGA
import com.proxerme.library.parameters.CommentStateParameter.*
import com.proxerme.library.parameters.MediumParameter.*
import com.proxerme.library.parameters.SeasonParameter.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object ParameterMapper {

    fun country(context: Context, it: String): Drawable {
        return AppCompatResources.getDrawable(context, when (it) {
            CountryParameter.ENGLISH -> R.drawable.ic_united_states
            CountryParameter.UNITED_STATES -> R.drawable.ic_united_states
            CountryParameter.GERMAN -> R.drawable.ic_germany
            CountryParameter.JAPANESE -> R.drawable.ic_japan
            CountryParameter.MISCELLANEOUS -> R.drawable.ic_united_nations
            else -> R.drawable.ic_error
        }) ?: throw IllegalStateException("Drawable inflation failed with parameter $it")
    }

    fun licence(context: Context, it: Int): String {
        return getSafeString(context, when (it) {
            LicenseParameter.LICENSED -> R.string.media_license_licensed
            LicenseParameter.NON_LICENSED -> R.string.media_license_non_licensed
            LicenseParameter.UNKNOWN -> R.string.media_license_unknown
            else -> null
        }, it)
    }

    fun fskDescription(context: Context, it: String): String {
        return getSafeString(context, when (it) {
            FskParameter.FSK_0 -> R.string.fsk_0_description
            FskParameter.FSK_6 -> R.string.fsk_6_description
            FskParameter.FSK_12 -> R.string.fsk_12_description
            FskParameter.FSK_16 -> R.string.fsk_16_description
            FskParameter.FSK_18 -> R.string.fsk_18_description
            FskParameter.BAD_LANGUAGE -> R.string.fsk_bad_language_description
            FskParameter.FEAR -> R.string.fsk_fear_description
            FskParameter.SEX -> R.string.fsk_sex_description
            FskParameter.VIOLENCE -> R.string.fsk_violence_description
            else -> null
        }, it)
    }

    fun fskImage(context: Context, it: String): Drawable {
        return AppCompatResources.getDrawable(context, when (it) {
            FskParameter.FSK_0 -> R.drawable.ic_fsk0
            FskParameter.FSK_6 -> R.drawable.ic_fsk6
            FskParameter.FSK_12 -> R.drawable.ic_fsk12
            FskParameter.FSK_16 -> R.drawable.ic_fsk16
            FskParameter.FSK_18 -> R.drawable.ic_fsk18
            FskParameter.BAD_LANGUAGE -> R.drawable.ic_bad_language
            FskParameter.FEAR -> R.drawable.ic_fear
            FskParameter.SEX -> R.drawable.ic_sex
            FskParameter.VIOLENCE -> R.drawable.ic_violence
            else -> R.drawable.ic_error
        }) ?: throw IllegalStateException("Drawable inflation failed with parameter $it")
    }

    fun mediaState(context: Context, it: Int): String {
        return getSafeString(context, when (it) {
            StateParameter.PRE_AIRING -> R.string.media_state_pre_airing
            StateParameter.AIRING -> R.string.media_state_airing
            StateParameter.CANCELLED -> R.string.media_state_cancelled
            StateParameter.CANCELLED_SUB -> R.string.media_state_cancelled_sub
            StateParameter.FINISHED -> R.string.media_state_finished
            else -> null
        }, it)
    }

    fun seasonStart(context: Context, it: EntrySeason): String {
        return when (it.season) {
            WINTER -> context.getString(R.string.fragment_media_season_winter_start, it.year)
            SPRING -> context.getString(R.string.fragment_media_season_spring_start, it.year)
            SUMMER -> context.getString(R.string.fragment_media_season_summer_start, it.year)
            AUTUMN -> context.getString(R.string.fragment_media_season_autumn_start, it.year)
            UNSPECIFIED -> it.year.toString()
            UNSPECIFIED_ALT -> it.year.toString()
            else -> context.getString(R.string.error_unknown_parameter, it)
        }
    }

    fun seasonEnd(context: Context, it: EntrySeason): String {
        return when (it.season) {
            WINTER -> context.getString(R.string.fragment_media_season_winter_end, it.year)
            SPRING -> context.getString(R.string.fragment_media_season_spring_end, it.year)
            SUMMER -> context.getString(R.string.fragment_media_season_summer_end, it.year)
            AUTUMN -> context.getString(R.string.fragment_media_season_autumn_end, it.year)
            UNSPECIFIED -> it.year.toString()
            UNSPECIFIED_ALT -> it.year.toString()
            else -> context.getString(R.string.error_unknown_parameter, it)
        }
    }

    fun rank(context: Context, it: Int): String {
        return getSafeString(context, when {
            (it < 10) -> R.string.rank_10
            (it < 100) -> R.string.rank_100
            (it < 200) -> R.string.rank_200
            (it < 500) -> R.string.rank_500
            (it < 700) -> R.string.rank_700
            (it < 1000) -> R.string.rank_1000
            (it < 1500) -> R.string.rank_1500
            (it < 2000) -> R.string.rank_2000
            (it < 3000) -> R.string.rank_3000
            (it < 4000) -> R.string.rank_4000
            (it < 6000) -> R.string.rank_6000
            (it < 8000) -> R.string.rank_8000
            (it < 10000) -> R.string.rank_10000
            (it < 11000) -> R.string.rank_11000
            (it < 12000) -> R.string.rank_12000
            (it < 14000) -> R.string.rank_14000
            (it < 16000) -> R.string.rank_16000
            (it < 18000) -> R.string.rank_18000
            (it < 20000) -> R.string.rank_20000
            (it > 20000) -> R.string.rank_kami_sama
            else -> null
        }, it)
    }

    fun projectState(context: Context, type: String, state: Int): String {
        return getSafeString(context, when (type.toIntOrNull()) {
            ProjectTypeParameter.UNDEFINED -> R.string.project_type_unkown
            ProjectTypeParameter.FINISHED -> R.string.project_type_finished
            ProjectTypeParameter.IN_WORK -> R.string.project_type_in_work
            ProjectTypeParameter.PLANNED -> R.string.project_type_planned
            ProjectTypeParameter.CANCELLED -> R.string.project_type_cancelled
            ProjectTypeParameter.LICENCED -> R.string.project_type_licenced
            null -> when (state) {
                StateParameter.PRE_AIRING -> R.string.media_state_pre_airing
                StateParameter.AIRING -> R.string.media_state_airing
                StateParameter.CANCELLED -> R.string.media_state_cancelled
                StateParameter.CANCELLED_SUB -> R.string.media_state_cancelled_sub
                StateParameter.FINISHED -> R.string.media_state_finished
                else -> null
            }
            else -> null
        }, type.toIntOrNull() ?: state)
    }

    fun commentState(context: Context, category: String?, state: Int, episode: Int): String {
        return when (state) {
            WATCHED -> return when (category) {
                ANIME -> context.getString(R.string.user_media_state_watched)
                MANGA -> context.getString(R.string.user_media_state_read)
                else -> context.getString(R.string.user_media_state_watched)
            }
            WATCHING -> return when (category) {
                ANIME -> context.getString(R.string.user_media_state_watching, episode)
                MANGA -> context.getString(R.string.user_media_state_reading, episode)
                else -> context.getString(R.string.user_media_state_watching, episode)
            }
            WILL_WATCH -> return when (category) {
                ANIME -> context.getString(R.string.user_media_state_will_watch)
                MANGA -> context.getString(R.string.user_media_state_will_read)
                else -> context.getString(R.string.user_media_state_will_watch)
            }
            CANCELLED -> context.getString(R.string.user_media_state_cancelled, episode)
            else -> context.getString(R.string.error_unknown_parameter, state.toString())
        }
    }

    fun mediaEpisodeCount(context: Context, category: String, count: Int): String {
        return when (category) {
            ANIME -> {
                context.resources.getQuantityString(R.plurals.media_episode_count, count, count)
            }
            MANGA -> {
                context.resources.getQuantityString(R.plurals.media_chapter_count, count, count)
            }
            else -> context.getString(R.string.error_unknown_parameter, category)
        }
    }

    fun medium(context: Context, it: String): String {
        return getSafeString(context, when (it) {
            ANIMESERIES -> R.string.media_medium_anime_series
            HENTAI -> R.string.media_medium_hentai
            MOVIE -> R.string.media_medium_movie
            OVA -> R.string.media_medium_ova
            MANGASERIES -> R.string.media_medium_manga_series
            DOUJIN -> R.string.media_medium_doujin
            HMANGA -> R.string.media_medium_h_manga
            ONESHOT -> R.string.media_medium_oneshot
            else -> null
        }, it)
    }

    fun mediumToCategory(medium: String): String? {
        return when (medium) {
            ANIMESERIES, HENTAI, MOVIE, OVA -> ANIME
            MANGASERIES, DOUJIN, HMANGA, ONESHOT -> MANGA
            else -> null
        }
    }

    private fun getSafeString(context: Context, resource: Int?, parameter: Any): String {
        return when (resource) {
            null -> context.getString(R.string.error_unknown_parameter, parameter)
            else -> context.getString(resource)
        }
    }
}