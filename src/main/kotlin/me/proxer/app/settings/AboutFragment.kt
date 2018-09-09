package me.proxer.app.settings

import android.content.Context
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.LibsConfiguration
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.R
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.forum.TopicActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.openHttpPage
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf
import org.koin.android.ext.android.inject
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class AboutFragment : MaterialAboutFragment() {

    companion object {
        private val LIBRARIES = arrayOf(
            "android_job", "android_textview_linkbuilder", "anko", "customtabshelper",
            "exomedia", "exoplayer", "flexboxlayout", "glide", "hawk", "kotterknife", "leakcanary",
            "materialbadgetextview", "materialdialogs", "materialprogressbar", "materialratingbar", "moshi",
            "okhttp", "retrofit", "rxandroid", "rxbinding", "rxlifecylce", "rxjava", "subsamplingscaleimageview",
            "tablayouthelper"
        )

        private val EXCLUDED_LIBRARIES = arrayOf("fastadapter", "materialize")

        private val FACEBOOK_LINK = Utils.getAndFixUrl("https://facebook.com/Anime.Proxer.Me")
        private val TWITTER_LINK = Utils.getAndFixUrl("https://twitter.com/proxerme")
        private val YOUTUBE_LINK = Utils.getAndFixUrl("https://youtube.com/channel/UC7h-fT9Y9XFxuZ5GZpbcrtA")
        private val DISCORD_LINK = Utils.getAndFixUrl("https://discord.gg/XwrEDmA")
        private val REPOSITORY_LINK = Utils.getAndFixUrl("https://github.com/proxer/ProxerAndroid")

        private const val SUPPORT_ID = "374605"
        private const val SUPPORT_CATEGORY = "anwendungen"

        private const val DEVELOPER_PROXER_NAME = "RubyGee"
        private const val DEVELOPER_PROXER_ID = "121658"
        private const val DEVELOPER_GITHUB_NAME = "rubengees"

        fun newInstance() = AboutFragment().apply {
            arguments = bundleOf()
        }
    }

    private val messengerDao by inject<MessengerDao>()

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList = MaterialAboutList.Builder()
        .addCard(MaterialAboutCard.Builder()
            .apply { buildInfoItems(context).forEach { addItem(it) } }
            .build())
        .addCard(MaterialAboutCard.Builder()
            .title(R.string.about_social_media_title)
            .apply { buildSocialMediaItems(context).forEach { addItem(it) } }
            .build())
        .addCard(MaterialAboutCard.Builder()
            .title(R.string.about_support_title)
            .apply { buildSupportItems(context).forEach { addItem(it) } }
            .build())
        .addCard(MaterialAboutCard.Builder()
            .title(getString(R.string.about_developer_title))
            .apply { buildDeveloperItems(context).forEach { addItem(it) } }
            .build())
        .build()

    override fun getTheme() = R.style.Theme_App_AboutFragment
    override fun shouldAnimate() = false

    private fun buildInfoItems(context: Context) = listOf(
        ConvenienceBuilder.createAppTitleItem(context),
        ConvenienceBuilder.createVersionActionItem(
            context,
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_tag).iconColor(context),
            getString(R.string.about_version_title), false
        ),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_licenses_title)
            .subText(R.string.about_licenses_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_clipboard_text).iconColor(context))
            .setOnClickAction {
                LibsBuilder().withAutoDetect(false)
                    .withShowLoadingProgress(false)
                    .withAboutVersionShown(false)
                    .withAboutIconShown(false)
                    .withAboutDescription(getString(R.string.about_licenses_activity_description))
                    .withLibraries(*LIBRARIES)
                    .withExcludedLibraries(*EXCLUDED_LIBRARIES)
                    .withFields(R.string::class.java.fields)
                    .withActivityStyle(getAboutLibrariesActivityStyle())
                    .withUiListener(NavigationBarLibsUIListener())
                    .withActivityTitle(getString(R.string.about_licenses_activity_title))
                    .start(requireActivity())
            }.build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_source_code)
            .subText(R.string.about_source_code_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_code_braces).iconColor(context))
            .setOnClickAction { showPage(REPOSITORY_LINK) }
            .build()
    )

    private fun buildSocialMediaItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .text(R.string.about_facebook_title)
            .subText(R.string.about_facebook_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_facebook).iconColor(context))
            .setOnClickAction { showPage(FACEBOOK_LINK) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_twitter_title)
            .subText(R.string.about_twitter_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_twitter).iconColor(context))
            .setOnClickAction { showPage(TWITTER_LINK) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_youtube_title)
            .subText(R.string.about_youtube_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_youtube_play).iconColor(context))
            .setOnClickAction { showPage(YOUTUBE_LINK) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_discord_title)
            .subText(R.string.about_discord_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_discord).iconColor(context))
            .setOnClickAction { showPage(DISCORD_LINK) }
            .build()
    )

    private fun buildSupportItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .text(R.string.about_support_forum_title)
            .subText(R.string.about_support_forum_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_forum).iconColor(context))
            .setOnClickAction { TopicActivity.navigateTo(requireActivity(), SUPPORT_ID, SUPPORT_CATEGORY) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_support_message_title)
            .subText(R.string.about_support_message_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_email).iconColor(context))
            .setOnClickAction {
                Completable
                    .fromAction {
                        messengerDao.findConferenceForUser(DEVELOPER_PROXER_NAME).let { existingConference ->
                            when (existingConference) {
                                null -> CreateConferenceActivity.navigateTo(
                                    requireActivity(), false, Participant(DEVELOPER_PROXER_NAME)
                                )
                                else -> MessengerActivity.navigateTo(requireActivity(), existingConference)
                            }
                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .subscribeAndLogErrors()
            }.build()
    )

    private fun buildDeveloperItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .text(R.string.about_developer_github_title)
            .subText(DEVELOPER_GITHUB_NAME)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_github_circle).iconColor(context))
            .setOnClickAction { showPage(Utils.getAndFixUrl("https://github.com/$DEVELOPER_GITHUB_NAME")) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(getString(R.string.about_developer_proxer_title))
            .subText(DEVELOPER_PROXER_NAME)
            .icon(ContextCompat.getDrawable(context, R.drawable.ic_stat_proxer)?.apply {
                setColorFilter(ContextCompat.getColor(context, R.color.icon), PorterDuff.Mode.SRC_IN)
            })
            .setOnClickAction {
                ProfileActivity.navigateTo(requireActivity(), DEVELOPER_PROXER_ID, DEVELOPER_PROXER_NAME, null)
            }
            .build()
    )

    private fun showPage(url: HttpUrl, forceBrowser: Boolean = false) {
        customTabsHelper.openHttpPage(requireActivity(), url, forceBrowser)
    }

    private fun getAboutLibrariesActivityStyle() =
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
            Configuration.UI_MODE_NIGHT_YES -> Libs.ActivityStyle.DARK
            Configuration.UI_MODE_NIGHT_UNDEFINED -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
            else -> throw IllegalArgumentException("Unknown mode")
        }

    private class NavigationBarLibsUIListener : LibsConfiguration.LibsUIListener {
        override fun preOnCreateView(view: View) = view
        override fun postOnCreateView(view: View) = view.apply {
            Utils.setNavigationBarColorIfPossible(Utils.findActivity(context), R.color.primary)
        }
    }
}
