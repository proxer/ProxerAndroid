package me.proxer.app.ucp.settings

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import me.proxer.library.entity.ucp.UcpSettings
import me.proxer.library.enums.UcpSettingConstraint
import me.proxer.library.enums.UcpSettingConstraint.DEFAULT

/**
 * @author Ruben Gees
 */
@JsonClass(generateAdapter = true)
data class LocalUcpSettings(
    @Json(name = "profileVisibility") val profileVisibility: UcpSettingConstraint,
    @Json(name = "topTenVisibility") val topTenVisibility: UcpSettingConstraint,
    @Json(name = "animeVisibility") val animeVisibility: UcpSettingConstraint,
    @Json(name = "mangaVisibility") val mangaVisibility: UcpSettingConstraint,
    @Json(name = "commentVisibility") val commentVisibility: UcpSettingConstraint,
    @Json(name = "forumVisibility") val forumVisibility: UcpSettingConstraint,
    @Json(name = "friendVisibility") val friendVisibility: UcpSettingConstraint,
    @Json(name = "friendRequestConstraint") val friendRequestConstraint: UcpSettingConstraint,
    @Json(name = "aboutVisibility") val aboutVisibility: UcpSettingConstraint,
    @Json(name = "historyVisibility") val historyVisibility: UcpSettingConstraint,
    @Json(name = "guestBookVisibility") val guestBookVisibility: UcpSettingConstraint,
    @Json(name = "guestBookEntryConstraint") val guestBookEntryConstraint: UcpSettingConstraint,
    @Json(name = "galleryVisibility") val galleryVisibility: UcpSettingConstraint,
    @Json(name = "articleVisibility") val articleVisibility: UcpSettingConstraint,
    @Json(name = "hide_tags") val shouldHideTags: Boolean,
    @Json(name = "voluntary_banner_ads_enabled") val shouldShowAds: Boolean,
    @Json(name = "voluntary_video_ads_interval") val adInterval: Int
) {

    companion object {
        fun default() = LocalUcpSettings(
            DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT, DEFAULT,
            DEFAULT, DEFAULT, false, false, 3
        )
    }

    fun toNonLocalSettings() = UcpSettings(
        profileVisibility, topTenVisibility, animeVisibility, mangaVisibility, commentVisibility, forumVisibility,
        friendVisibility, friendRequestConstraint, aboutVisibility, historyVisibility, guestBookVisibility,
        guestBookEntryConstraint, galleryVisibility, articleVisibility, shouldHideTags, shouldShowAds, adInterval
    )
}
