@file:Suppress("MethodOverloading")

package me.proxer.app.util.extension

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.content.res.AppCompatResources
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import me.proxer.app.R
import me.proxer.app.R.id.description
import me.proxer.app.R.id.post
import me.proxer.app.anime.AnimeStream
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.pub.message.ParsedChatMessage
import me.proxer.app.forum.ParsedPost
import me.proxer.app.forum.TopicMetaData
import me.proxer.app.media.LocalTag
import me.proxer.app.media.comment.ParsedComment
import me.proxer.app.profile.comment.ParsedUserComment
import me.proxer.app.ucp.settings.LocalUcpSettings
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBParser
import me.proxer.app.util.Utils
import me.proxer.library.entity.anime.Stream
import me.proxer.library.entity.chat.ChatMessage
import me.proxer.library.entity.forum.Post
import me.proxer.library.entity.forum.Topic
import me.proxer.library.entity.info.Comment
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.entity.info.EntrySeasonInfo
import me.proxer.library.entity.info.Synonym
import me.proxer.library.entity.list.Tag
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.entity.manga.Page
import me.proxer.library.entity.messenger.Conference
import me.proxer.library.entity.messenger.Message
import me.proxer.library.entity.ucp.UcpSettings
import me.proxer.library.entity.user.UserComment
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.CalendarDay
import me.proxer.library.enums.Category
import me.proxer.library.enums.Country
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Gender
import me.proxer.library.enums.IndustryType
import me.proxer.library.enums.Language
import me.proxer.library.enums.License
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.MediaType
import me.proxer.library.enums.Medium
import me.proxer.library.enums.MessageAction
import me.proxer.library.enums.RelationshipStatus
import me.proxer.library.enums.Season
import me.proxer.library.enums.SynonymType
import me.proxer.library.enums.UserMediaProgress
import java.net.URLDecoder

object ProxerLibExtensions {

    fun fskConstraintFromAppString(context: Context, string: String) = when (string) {
        context.getString(R.string.fsk_0) -> FskConstraint.FSK_0
        context.getString(R.string.fsk_6) -> FskConstraint.FSK_6
        context.getString(R.string.fsk_12) -> FskConstraint.FSK_12
        context.getString(R.string.fsk_16) -> FskConstraint.FSK_16
        context.getString(R.string.fsk_18) -> FskConstraint.FSK_18
        context.getString(R.string.fsk_bad_language) -> FskConstraint.BAD_LANGUAGE
        context.getString(R.string.fsk_fear) -> FskConstraint.FEAR
        context.getString(R.string.fsk_violence) -> FskConstraint.VIOLENCE
        context.getString(R.string.fsk_sex) -> FskConstraint.SEX
        else -> throw IllegalStateException("Could not find fsk constraint for description: $description")
    }
}

fun Medium.toAppString(context: Context): String = context.getString(
    when (this) {
        Medium.ANIMESERIES -> R.string.medium_anime_series
        Medium.HENTAI -> R.string.medium_hentai
        Medium.MOVIE -> R.string.medium_movie
        Medium.OVA -> R.string.medium_ova
        Medium.MANGASERIES -> R.string.medium_manga_series
        Medium.DOUJIN -> R.string.medium_doujin
        Medium.HMANGA -> R.string.medium_h_manga
        Medium.ONESHOT -> R.string.medium_oneshot
    }
)

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

fun Language.toAppDrawable(context: Context) = AppCompatResources.getDrawable(
    context, when (this) {
        Language.GERMAN -> R.drawable.ic_germany
        Language.ENGLISH -> R.drawable.ic_united_states
        Language.OTHER -> R.drawable.ic_united_nations
    }
) ?: throw IllegalStateException("Could not resolve Drawable for language: $this")

fun MediaLanguage.toAppString(context: Context): String = context.getString(
    when (this) {
        MediaLanguage.GERMAN -> R.string.language_german
        MediaLanguage.ENGLISH -> R.string.language_english
        MediaLanguage.GERMAN_SUB -> R.string.language_german_sub
        MediaLanguage.GERMAN_DUB -> R.string.language_german_dub
        MediaLanguage.ENGLISH_SUB -> R.string.language_english_sub
        MediaLanguage.ENGLISH_DUB -> R.string.language_english_dub
        MediaLanguage.OTHER -> R.string.language_other
    }
)

fun Country.toAppDrawable(context: Context) = AppCompatResources.getDrawable(
    context, when (this) {
        Country.GERMANY -> R.drawable.ic_germany
        Country.ENGLAND -> R.drawable.ic_united_states
        Country.UNITED_STATES -> R.drawable.ic_united_states
        Country.JAPAN -> R.drawable.ic_japan
        Country.KOREA -> R.drawable.ic_korea
        Country.CHINA -> R.drawable.ic_china
        Country.INTERNATIONAL, Country.OTHER, Country.NONE -> R.drawable.ic_united_nations
    }
) ?: throw IllegalStateException("Could not resolve Drawable for country: $this")

fun Medium.toCategory() = when (this) {
    Medium.ANIMESERIES, Medium.MOVIE, Medium.OVA, Medium.HENTAI -> Category.ANIME
    Medium.MANGASERIES, Medium.ONESHOT, Medium.DOUJIN, Medium.HMANGA -> Category.MANGA
}

fun Category.toEpisodeAppString(context: Context, number: Int? = null): String = when (number) {
    null -> context.getString(
        when (this) {
            Category.ANIME -> R.string.category_anime_episodes_title
            Category.MANGA -> R.string.category_manga_episodes_title
        }
    )
    else -> context.getString(
        when (this) {
            Category.ANIME -> R.string.category_anime_episode_number
            Category.MANGA -> R.string.category_manga_episode_number
        },
        number
    )
}

@Suppress("USELESS_CAST")
fun MediaState.toAppDrawable(context: Context): IconicsDrawable = IconicsDrawable(context)
    .iconColor(context)
    .icon(
        when (this) {
            MediaState.PRE_AIRING -> CommunityMaterial.Icon2.cmd_radio_tower as IIcon
            MediaState.FINISHED -> CommunityMaterial.Icon.cmd_book
            MediaState.AIRING -> CommunityMaterial.Icon.cmd_book_open_variant
            MediaState.CANCELLED -> CommunityMaterial.Icon.cmd_close
            MediaState.CANCELLED_SUB -> CommunityMaterial.Icon.cmd_close
        }
    )

fun UserMediaProgress.toEpisodeAppString(
    context: Context,
    episode: Int = 1,
    category: Category = Category.ANIME
): String = when (this) {
    UserMediaProgress.WATCHED -> context.getString(
        when (category) {
            Category.ANIME -> R.string.user_media_progress_watched
            Category.MANGA -> R.string.user_media_progress_read
        }
    )
    UserMediaProgress.WATCHING -> context.getString(
        when (category) {
            Category.ANIME -> R.string.user_media_progress_watching
            Category.MANGA -> R.string.user_media_progress_reading
        },
        episode
    )
    UserMediaProgress.WILL_WATCH -> context.getString(
        when (category) {
            Category.ANIME -> R.string.user_media_progress_will_watch
            Category.MANGA -> R.string.user_media_progress_will_read
        }
    )
    UserMediaProgress.CANCELLED -> context.getString(R.string.user_media_progress_cancelled, episode)
}

fun Synonym.toTypeAppString(context: Context): String = context.getString(
    when (this.type) {
        SynonymType.ORIGINAL -> R.string.synonym_original_type
        SynonymType.ENGLISH -> R.string.synonym_english_type
        SynonymType.GERMAN -> R.string.synonym_german_type
        SynonymType.JAPANESE -> R.string.synonym_japanese_type
        SynonymType.KOREAN -> R.string.synonym_korean_type
        SynonymType.CHINESE -> R.string.synonym_chinese_type
        SynonymType.ORIGINAL_ALTERNATIVE -> R.string.synonym_alternative_type
        SynonymType.OTHER -> R.string.synonym_alternative_type
    }
)

fun EntrySeasonInfo.toStartAppString(context: Context): String = when (season) {
    Season.WINTER -> context.getString(R.string.season_winter_start, year)
    Season.SPRING -> context.getString(R.string.season_spring_start, year)
    Season.SUMMER -> context.getString(R.string.season_summer_start, year)
    Season.AUTUMN -> context.getString(R.string.season_autumn_start, year)
    Season.UNSPECIFIED -> year.toString()
}

fun EntrySeasonInfo.toEndAppString(context: Context): String = when (season) {
    Season.WINTER -> context.getString(R.string.season_winter_end, year)
    Season.SPRING -> context.getString(R.string.season_spring_end, year)
    Season.SUMMER -> context.getString(R.string.season_summer_end, year)
    Season.AUTUMN -> context.getString(R.string.season_autumn_end, year)
    Season.UNSPECIFIED -> year.toString()
}

fun MediaState.toAppString(context: Context): String = context.getString(
    when (this) {
        MediaState.PRE_AIRING -> R.string.media_state_pre_airing
        MediaState.AIRING -> R.string.media_state_airing
        MediaState.CANCELLED -> R.string.media_state_cancelled
        MediaState.CANCELLED_SUB -> R.string.media_state_cancelled_sub
        MediaState.FINISHED -> R.string.media_state_finished
    }
)

fun License.toAppString(context: Context): String = context.getString(
    when (this) {
        License.LICENSED -> R.string.license_licensed
        License.NOT_LICENSED -> R.string.license_not_licensed
        License.UNKNOWN -> R.string.license_unknown
    }
)

fun FskConstraint.toAppString(context: Context): String = context.getString(
    when (this) {
        FskConstraint.FSK_0 -> R.string.fsk_0
        FskConstraint.FSK_6 -> R.string.fsk_6
        FskConstraint.FSK_12 -> R.string.fsk_12
        FskConstraint.FSK_16 -> R.string.fsk_16
        FskConstraint.FSK_18 -> R.string.fsk_18
        FskConstraint.BAD_LANGUAGE -> R.string.fsk_bad_language
        FskConstraint.FEAR -> R.string.fsk_fear
        FskConstraint.VIOLENCE -> R.string.fsk_violence
        FskConstraint.SEX -> R.string.fsk_sex
    }
)

fun FskConstraint.toAppStringDescription(context: Context): String = context.getString(
    when (this) {
        FskConstraint.FSK_0 -> R.string.fsk_0_description
        FskConstraint.FSK_6 -> R.string.fsk_6_description
        FskConstraint.FSK_12 -> R.string.fsk_12_description
        FskConstraint.FSK_16 -> R.string.fsk_16_description
        FskConstraint.FSK_18 -> R.string.fsk_18_description
        FskConstraint.BAD_LANGUAGE -> R.string.fsk_bad_language_description
        FskConstraint.FEAR -> R.string.fsk_fear_description
        FskConstraint.VIOLENCE -> R.string.fsk_violence_description
        FskConstraint.SEX -> R.string.fsk_sex_description
    }
)

fun FskConstraint.toAppDrawable(context: Context) = AppCompatResources.getDrawable(
    context, when (this) {
        FskConstraint.FSK_0 -> R.drawable.ic_fsk_0
        FskConstraint.FSK_6 -> R.drawable.ic_fsk_6
        FskConstraint.FSK_12 -> R.drawable.ic_fsk_12
        FskConstraint.FSK_16 -> R.drawable.ic_fsk_16
        FskConstraint.FSK_18 -> R.drawable.ic_fsk_18
        FskConstraint.BAD_LANGUAGE -> R.drawable.ic_fsk_bad_language
        FskConstraint.FEAR -> R.drawable.ic_fsk_fear
        FskConstraint.SEX -> R.drawable.ic_fsk_sex
        FskConstraint.VIOLENCE -> R.drawable.ic_fsk_violence
    }
) ?: throw IllegalStateException("Could not resolve Drawable for fsk constraint: $this")

fun IndustryType.toAppString(context: Context): String = context.getString(
    when (this) {
        IndustryType.PUBLISHER -> R.string.industry_publisher
        IndustryType.STUDIO -> R.string.industry_studio
        IndustryType.STUDIO_SECONDARY -> R.string.industry_studio_secondary
        IndustryType.PRODUCER -> R.string.industry_producer
        IndustryType.RECORD_LABEL -> R.string.industry_record_label
        IndustryType.TALENT_AGENT -> R.string.industry_talent_agent
        IndustryType.STREAMING -> R.string.industry_streaming
        IndustryType.DEVELOPER -> R.string.industry_developer
        IndustryType.TV -> R.string.industry_tv
        IndustryType.SOUND_STUDIO -> R.string.industry_sound_studio
        IndustryType.UNKNOWN -> R.string.industry_unknown
    }
)

fun CalendarDay.toAppString(context: Context): String = context.getString(
    when (this) {
        CalendarDay.MONDAY -> R.string.day_monday
        CalendarDay.TUESDAY -> R.string.day_tuesday
        CalendarDay.WEDNESDAY -> R.string.day_wednesday
        CalendarDay.THURSDAY -> R.string.day_thursday
        CalendarDay.FRIDAY -> R.string.day_friday
        CalendarDay.SATURDAY -> R.string.day_saturday
        CalendarDay.SUNDAY -> R.string.day_sunday
    }
)

fun Gender.toAppString(context: Context): String = context.getString(
    when (this) {
        Gender.MALE -> R.string.gender_male
        Gender.FEMALE -> R.string.gender_female
        Gender.OTHER -> R.string.gender_other
        Gender.UNKNOWN -> R.string.gender_unknown
    }
)

fun RelationshipStatus.toAppString(context: Context): String = context.getString(
    when (this) {
        RelationshipStatus.SINGLE -> R.string.relationship_status_single
        RelationshipStatus.IN_RELATION -> R.string.relationship_status_in_relation
        RelationshipStatus.ENGAGED -> R.string.relationship_status_engaged
        RelationshipStatus.COMPLICATED -> R.string.relationship_status_complicated
        RelationshipStatus.MARRIED -> R.string.relationship_status_married
        RelationshipStatus.SEARCHING -> R.string.relationship_status_searching
        RelationshipStatus.NOT_SEARCHING -> R.string.relationship_status_not_searching
        RelationshipStatus.UNKNOWN -> R.string.relationship_status_unknown
    }
)

fun MessageAction.toAppString(context: Context, username: String, message: String): String = when (this) {
    MessageAction.ADD_USER -> context.getString(R.string.action_conference_add_user, "@$username", "@$message")
    MessageAction.REMOVE_USER -> context.getString(R.string.action_conference_delete_user, "@$username", "@$message")
    MessageAction.SET_LEADER -> context.getString(R.string.action_conference_set_leader, "@$username", "@$message")
    MessageAction.SET_TOPIC -> context.getString(R.string.action_conference_set_topic, "@$username", message)
    MessageAction.NONE -> message
}

fun MediaType.isAgeRestricted(): Boolean {
    return this == MediaType.ALL_WITH_HENTAI || this == MediaType.HENTAI || this == MediaType.HMANGA
}

inline val Chapter.isOfficial: Boolean
    get() = if (pages == null) {
        val serverUrl = Utils.parseAndFixUrl(server)

        serverUrl != null && serverUrl.host() in arrayOf("www.webtoons.com", "www.lezhin.com")
    } else {
        false
    }

inline val Entry.isTrulyAgeRestricted: Boolean
    get() = isAgeRestricted || medium == Medium.HMANGA || medium == Medium.HENTAI ||
        fskConstraints.contains(FskConstraint.FSK_18)

inline val EntryCore.isAgeRestricted: Boolean
    get() = medium == Medium.HMANGA || medium == Medium.HENTAI || fskConstraints.contains(FskConstraint.FSK_18)

inline val Page.decodedName: String
    get() = try {
        URLDecoder.decode(name, "UTF-8")
    } catch (error: Throwable) {
        ""
    }

fun Stream.toAnimeStreamInfo(isSupported: Boolean, isInternalPlayerOnly: Boolean) = AnimeStream(
    id, hoster, hosterName, image, uploaderId, uploaderName, date, translatorGroupId, translatorGroupName,
    isOfficial, isPublic, isSupported, isInternalPlayerOnly
)

fun Conference.toLocalConference(isFullyLoaded: Boolean) = LocalConference(
    id.toLong(), topic, customTopic, participantAmount, image, imageType, isGroup, isRead, isRead, date,
    unreadMessageAmount, lastReadMessageId, isFullyLoaded
)

fun UcpSettings.toLocalSettings() = LocalUcpSettings(
    profileVisibility, topTenVisibility, animeVisibility, mangaVisibility, commentVisibility, forumVisibility,
    friendVisibility, friendRequestConstraint, aboutVisibility, historyVisibility, guestBookVisibility,
    guestBookEntryConstraint, galleryVisibility, articleVisibility, isHideTags, isShowAds, adInterval
)

fun Message.toLocalMessage() = LocalMessage(
    id.toLong(), conferenceId.toLong(), userId, username, message, action, date, device
)

fun Comment.toParsedComment() = ParsedComment(
    id, entryId, authorId, mediaProgress, ratingDetails,
    BBParser.parseSimple(content).optimize(), overallRating, episode, helpfulVotes, date, author, image
)

fun UserComment.toParsedUserComment() = ParsedUserComment(
    id, entryId, entryName, medium, category, authorId, mediaProgress, ratingDetails,
    BBParser.parseSimple(content).optimize(), overallRating, episode, helpfulVotes, date, author, image
)

fun Topic.toTopicMetaData() = TopicMetaData(
    categoryId, categoryName, firstPostDate, lastPostDate, hits, isLocked, post, subject
)

fun Post.toParsedPost(resources: Resources): ParsedPost {
    val parsedMessage = BBParser.parse(message).optimize(BBArgs(resources = resources, userId = userId))
    val parsedSignature = signature?.let {
        if (it.isNotBlank()) BBParser.parse(it).optimize(BBArgs(resources = resources, userId = userId)) else null
    }

    return ParsedPost(
        id, parentId, userId, username, image, date, parsedSignature, modifiedById, modifiedByName, modifiedReason,
        parsedMessage, thankYouAmount
    )
}

fun Tag.toParcelableTag() = LocalTag(id, type, name, description, subType, isSpoiler)

fun ChatMessage.toParsedMessage() = ParsedChatMessage(id, userId, username, image, message, action, date)
