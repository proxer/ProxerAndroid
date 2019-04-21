package me.proxer.app.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.base.CustomTabsAware
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.forum.TopicActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.settings.status.ServerStatusActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.openHttpPage
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toast
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.koin.android.ext.android.inject
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class AboutFragment : MaterialAboutFragment(), CustomTabsAware {

    companion object {
        private val libraries = arrayOf(
            "aspectratioimageview", "autodispose", "betterlinkmovementmethod", "customtabshelper", "exoplayer",
            "flexboxlayout", "glide", "hawk", "jsoup", "koptional", "kotterknife", "leakcanary", "materialaboutlibrary",
            "materialpreference", "materialdialogs", "materialprogressbar", "materialratingbar", "moshi", "okhttp",
            "rapiddecoder", "recyclerviewsnap", "retrofit", "rxandroid", "rxbinding", "rxkotlin", "rxjava",
            "subsamplingpdfdecoder", "subsamplingscaleimageview", "tablayouthelper", "timber", "threeten",
            "threetenandroidbackport"
        )

        private val excludedLibraries = arrayOf("fastadapter", "materialize")

        private val facebookLink = Utils.getAndFixUrl("https://facebook.com/Anime.Proxer.Me")
        private val twitterLink = Utils.getAndFixUrl("https://twitter.com/proxerme")
        private val youtubeLink = Utils.getAndFixUrl("https://youtube.com/channel/UC7h-fT9Y9XFxuZ5GZpbcrtA")
        private val discordLink = Utils.getAndFixUrl("https://discord.gg/XwrEDmA")
        private val repositoryLink = Utils.getAndFixUrl("https://github.com/proxer/ProxerAndroid")

        private const val supportId = "374605"
        private const val supportCategory = "anwendungen"

        private const val developerProxerName = "RubyGee"
        private const val developerProxerId = "121658"
        private const val developerGithubName = "rubengees"

        fun newInstance() = AboutFragment().apply {
            arguments = bundleOf()
        }
    }

    private val preferenceHelper by inject<PreferenceHelper>()
    private val messengerDao by inject<MessengerDao>()

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean) {
        customTabsHelper.openHttpPage(requireActivity(), url, forceBrowser)
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

    override fun getTheme() = R.style.Fragment_App_AboutFragment
    override fun shouldAnimate() = false

    private fun buildInfoItems(context: Context) = listOf(
        ConvenienceBuilder.createAppTitleItem(context),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_version_title)
            .subText(BuildConfig.VERSION_NAME)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon2.cmd_tag).iconColor(context))
            .setOnClickAction {
                val title = getString(R.string.clipboard_title)
                val clipData = ClipData.newPlainText(title, BuildConfig.VERSION_NAME)

                requireContext().getSystemService<ClipboardManager>()?.primaryClip = clipData
                requireContext().toast(R.string.clipboard_status)
            }.build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_licenses_title)
            .subText(R.string.about_licenses_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_clipboard_text).iconColor(context))
            .setOnClickAction {
                LibsBuilder()
                    .withAutoDetect(false)
                    .withShowLoadingProgress(false)
                    .withAboutVersionShown(false)
                    .withAboutIconShown(false)
                    .withLibraries(*libraries)
                    .withExcludedLibraries(*excludedLibraries)
                    .withFields(R.string::class.java.fields)
                    .withActivityTheme(preferenceHelper.themeContainer.theme.main)
                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                    .withActivityTitle(getString(R.string.about_licenses_activity_title))
                    .start(requireActivity())
            }.build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_source_code)
            .subText(R.string.about_source_code_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_code_braces).iconColor(context))
            .setOnClickAction { showPage(repositoryLink) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_server_status)
            .subText(R.string.about_server_status_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon2.cmd_server).iconColor(context))
            .setOnClickAction { ServerStatusActivity.navigateTo(requireActivity()) }
            .build()
    )

    private fun buildSocialMediaItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .text(R.string.about_facebook_title)
            .subText(R.string.about_facebook_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_facebook).iconColor(context))
            .setOnClickAction { showPage(facebookLink) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_twitter_title)
            .subText(R.string.about_twitter_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon2.cmd_twitter).iconColor(context))
            .setOnClickAction { showPage(twitterLink) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_youtube_title)
            .subText(R.string.about_youtube_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon2.cmd_youtube).iconColor(context))
            .setOnClickAction { showPage(youtubeLink) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_discord_title)
            .subText(R.string.about_discord_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_discord).iconColor(context))
            .setOnClickAction { showPage(discordLink) }
            .build()
    )

    private fun buildSupportItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .text(R.string.about_support_forum_title)
            .subText(R.string.about_support_forum_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_forum).iconColor(context))
            .setOnClickAction { TopicActivity.navigateTo(requireActivity(), supportId, supportCategory) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_support_message_title)
            .subText(R.string.about_support_message_description)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_email).iconColor(context))
            .setOnClickAction {
                Completable
                    .fromAction {
                        messengerDao.findConferenceForUser(developerProxerName).let { existingConference ->
                            when (existingConference) {
                                null -> CreateConferenceActivity.navigateTo(
                                    requireActivity(), false, Participant(developerProxerName)
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
            .subText(developerGithubName)
            .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_github_circle).iconColor(context))
            .setOnClickAction { showPage(Utils.getAndFixUrl("https://github.com/$developerGithubName")) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(getString(R.string.about_developer_proxer_title))
            .subText(developerProxerName)
            .icon(ContextCompat.getDrawable(context, R.drawable.ic_stat_proxer)?.apply {
                setColorFilter(context.resolveColor(R.attr.colorIcon), PorterDuff.Mode.SRC_IN)
            })
            .setOnClickAction {
                ProfileActivity.navigateTo(requireActivity(), developerProxerId, developerProxerName, null)
            }
            .build()
    )
}
