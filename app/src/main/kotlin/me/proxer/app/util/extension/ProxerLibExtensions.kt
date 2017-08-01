package me.proxer.app.util.extension

import android.content.Context
import android.support.v7.content.res.AppCompatResources
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.manga.local.LocalEntryCore
import me.proxer.app.manga.local.LocalMangaChapter
import me.proxer.app.manga.local.LocalMangaPage
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.entitiy.info.EntrySeasonInfo
import me.proxer.library.entitiy.info.Synonym
import me.proxer.library.entitiy.manga.Chapter
import me.proxer.library.entitiy.manga.Page
import me.proxer.library.enums.*
import java.net.URLDecoder

fun Medium.toAppString(context: Context): String = context.getString(when (this) {
    Medium.ANIMESERIES -> R.string.medium_anime_series
    Medium.HENTAI -> R.string.medium_hentai
    Medium.MOVIE -> R.string.medium_movie
    Medium.OVA -> R.string.medium_ova
    Medium.MANGASERIES -> R.string.medium_manga_series
    Medium.DOUJIN -> R.string.medium_doujin
    Medium.HMANGA -> R.string.medium_h_manga
    Medium.ONESHOT -> R.string.medium_oneshot
})

fun MediaLanguage.toGeneralLanguage() = when (this) {
    MediaLanguage.GERMAN, MediaLanguage.GERMAN_SUB, MediaLanguage.GERMAN_DUB -> Language.GERMAN
    MediaLanguage.ENGLISH, MediaLanguage.ENGLISH_SUB, MediaLanguage.ENGLISH_DUB -> Language.ENGLISH
    MediaLanguage.OTHER -> Language.OTHER
}

fun MediaLanguage.toAnimeLanguage() = when (this) {
    MediaLanguage.GERMAN, MediaLanguage.GERMAN_SUB -> AnimeLanguage.GERMAN_SUB
    MediaLanguage.ENGLISH, MediaLanguage.ENGLISH_SUB -> AnimeLanguage.ENGLISH_SUB
    MediaLanguage.GERMAN_DUB -> AnimeLanguage.GERMAN_DUB
    MediaLanguage.ENGLISH_DUB -> AnimeLanguage.ENGLISH_DUB
    MediaLanguage.OTHER -> AnimeLanguage.OTHER
}

fun Language.toMediaLanguage() = when (this) {
    Language.GERMAN -> MediaLanguage.GERMAN
    Language.ENGLISH -> MediaLanguage.ENGLISH
    Language.OTHER -> MediaLanguage.OTHER
}

fun AnimeLanguage.toMediaLanguage() = when (this) {
    AnimeLanguage.GERMAN_SUB -> MediaLanguage.GERMAN_SUB
    AnimeLanguage.GERMAN_DUB -> MediaLanguage.GERMAN_DUB
    AnimeLanguage.ENGLISH_SUB -> MediaLanguage.ENGLISH_SUB
    AnimeLanguage.ENGLISH_DUB -> MediaLanguage.ENGLISH_DUB
    AnimeLanguage.OTHER -> MediaLanguage.OTHER
}

fun Language.toAppDrawable(context: Context) = AppCompatResources.getDrawable(context, when (this) {
    Language.GERMAN -> R.drawable.ic_germany
    Language.ENGLISH -> R.drawable.ic_united_states
    Language.OTHER -> R.drawable.ic_united_nations
}) ?: throw NullPointerException("Could not resolve Drawable for language: $this")

fun MediaLanguage.toAppString(context: Context): String = context.getString(when (this) {
    MediaLanguage.GERMAN -> R.string.language_german
    MediaLanguage.ENGLISH -> R.string.language_english
    MediaLanguage.GERMAN_SUB -> R.string.language_german_sub
    MediaLanguage.GERMAN_DUB -> R.string.language_german_dub
    MediaLanguage.ENGLISH_SUB -> R.string.language_english_sub
    MediaLanguage.ENGLISH_DUB -> R.string.language_english_dub
    MediaLanguage.OTHER -> R.string.language_other
})

fun Country.toAppDrawable(context: Context) = AppCompatResources.getDrawable(context, when (this) {
    Country.GERMANY -> R.drawable.ic_germany
    Country.ENGLAND -> R.drawable.ic_united_states
    Country.UNITED_STATES -> R.drawable.ic_united_states
    Country.JAPAN -> R.drawable.ic_japan
    Country.KOREA -> R.drawable.ic_korea
    Country.CHINA -> R.drawable.ic_china
    Country.OTHER -> R.drawable.ic_united_nations
}) ?: throw NullPointerException("Could not resolve Drawable for country: $this")

fun Medium.toCategory() = when (this) {
    Medium.ANIMESERIES, Medium.MOVIE, Medium.OVA, Medium.HENTAI -> Category.ANIME
    Medium.MANGASERIES, Medium.ONESHOT, Medium.DOUJIN, Medium.HMANGA -> Category.MANGA
}

fun Category.toEpisodeAppString(context: Context, number: Int? = null): String = when (number) {
    null -> context.getString(when (this) {
        Category.ANIME -> R.string.category_anime_episodes_title
        Category.MANGA -> R.string.category_manga_episodes_title
    })
    else -> context.getString(when (this) {
        Category.ANIME -> R.string.category_anime_episode_number
        Category.MANGA -> R.string.category_manga_episode_number
    }, number)
}

fun MediaState.toAppDrawable(context: Context): IconicsDrawable = IconicsDrawable(context)
        .colorRes(R.color.icon)
        .icon(when (this) {
            MediaState.PRE_AIRING -> CommunityMaterial.Icon.cmd_radio_tower
            MediaState.FINISHED -> CommunityMaterial.Icon.cmd_book
            MediaState.AIRING -> CommunityMaterial.Icon.cmd_book_open_variant
            MediaState.CANCELLED -> CommunityMaterial.Icon.cmd_close
            MediaState.CANCELLED_SUB -> CommunityMaterial.Icon.cmd_close
        })

fun UserMediaProgress.toEpisodeAppString(context: Context, episode: Int = 1,
                                         category: Category = Category.ANIME): String = when (this) {
    UserMediaProgress.WATCHED -> context.getString(when (category) {
        Category.ANIME -> R.string.user_media_progress_watched
        Category.MANGA -> R.string.user_media_progress_read
    })
    UserMediaProgress.WATCHING -> context.getString(when (category) {
        Category.ANIME -> R.string.user_media_progress_watching
        Category.MANGA -> R.string.user_media_progress_reading
    }, episode)
    UserMediaProgress.WILL_WATCH -> context.getString(when (category) {
        Category.ANIME -> R.string.user_media_progress_will_watch
        Category.MANGA -> R.string.user_media_progress_will_read
    })
    UserMediaProgress.CANCELLED -> context.getString(R.string.user_media_progress_cancelled, episode)
}

fun Synonym.toTypeAppString(context: Context): String = context.getString(when (this.type) {
    SynonymType.ORIGINAL -> R.string.synonym_original_type
    SynonymType.ENGLISH -> R.string.synonym_english_type
    SynonymType.GERMAN -> R.string.synonym_german_type
    SynonymType.JAPANESE -> R.string.synonym_japanese_type
    SynonymType.KOREAN -> R.string.synonym_korean_type
    SynonymType.CHINESE -> R.string.synonym_chinese_type
    SynonymType.ORIGINAL_ALTERNATIVE -> R.string.synonym_alternative_type
})

fun EntrySeasonInfo.toStartAppString(context: Context): String = when (season) {
    Season.WINTER -> context.getString(R.string.season_winter_start, year)
    Season.SPRING -> context.getString(R.string.season_spring_start, year)
    Season.SUMMER -> context.getString(R.string.season_summer_start, year)
    Season.AUTUMN -> context.getString(R.string.season_autumn_start, year)
    Season.UNSPECIFIED -> year.toString()
    Season.UNSPECIFIED_ALT -> year.toString()
}

fun EntrySeasonInfo.toEndAppString(context: Context): String = when (season) {
    Season.WINTER -> context.getString(R.string.season_winter_end, year)
    Season.SPRING -> context.getString(R.string.season_spring_end, year)
    Season.SUMMER -> context.getString(R.string.season_summer_end, year)
    Season.AUTUMN -> context.getString(R.string.season_autumn_end, year)
    Season.UNSPECIFIED -> year.toString()
    Season.UNSPECIFIED_ALT -> year.toString()
}

fun MediaState.toAppString(context: Context): String = context.getString(when (this) {
    MediaState.PRE_AIRING -> R.string.media_state_pre_airing
    MediaState.AIRING -> R.string.media_state_airing
    MediaState.CANCELLED -> R.string.media_state_cancelled
    MediaState.CANCELLED_SUB -> R.string.media_state_cancelled_sub
    MediaState.FINISHED -> R.string.media_state_finished
})

fun License.toAppString(context: Context): String = context.getString(when (this) {
    License.LICENSED -> R.string.license_licensed
    License.NOT_LICENSED -> R.string.license_not_licensed
    License.UNKNOWN -> R.string.license_unknown
})

fun FskConstraint.toAppStringDescription(context: Context): String = context.getString(when (this) {
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

fun FskConstraint.toAppDrawable(context: Context) = AppCompatResources.getDrawable(context, when (this) {
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

inline val Page.decodedName: String
    get() = try {
        URLDecoder.decode(name, "UTF-8")
    } catch (error: Throwable) {
        ""
    }

fun EntryCore.toLocalEntryCore() = LocalEntryCore(id.toLong(), name, genres, fskConstraints, description, medium,
        episodeAmount, state, ratingSum, ratingAmount, clicks, category, license)

fun Chapter.toLocalChapter(episode: Int, language: Language) = LocalMangaChapter(id.toLong(), episode, language,
        entryId, title, uploaderId, uploaderName, date, scanGroupId, scanGroupName, server)

fun Page.toLocalPage(id: Long = 0, chapterId: Long) = LocalMangaPage(id, name, height, width, chapterId)
