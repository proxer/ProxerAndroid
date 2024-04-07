package me.proxer.app.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.base.CustomTabsAware
import me.proxer.app.settings.status.ServerStatusActivity
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.toPrefixedHttpUrl
import me.proxer.app.util.extension.toast
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class AboutFragment : MaterialAboutFragment(), CustomTabsAware {

    companion object {
        private val teamLink = "https://proxer.me/team?device=default".toPrefixedHttpUrl()
        private val facebookLink = "https://facebook.com/Anime.Proxer.Me".toPrefixedHttpUrl()
        private val twitterLink = "https://twitter.com/proxerme".toPrefixedHttpUrl()
        private val youtubeLink = "https://youtube.com/channel/UC7h-fT9Y9XFxuZ5GZpbcrtA".toPrefixedHttpUrl()
        private val discordLink = "https://discord.gg/XwrEDmA".toPrefixedHttpUrl()
        private val repositoryLink = "https://github.com/proxer/ProxerAndroid".toPrefixedHttpUrl()

        private const val supportProxerMail = "appsupport@proxer.de"
        private const val developerGithubName = "rubengees"

        fun newInstance() = AboutFragment().apply {
            arguments = bundleOf()
        }
    }

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)

        setLikelyUrl(teamLink)
        setLikelyUrl(facebookLink)
        setLikelyUrl(twitterLink)
        setLikelyUrl(youtubeLink)
        setLikelyUrl(discordLink)
    }

    override fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean, skipCheck: Boolean) {
        customTabsHelper.fallbackHandleLink(requireActivity(), url, forceBrowser, skipCheck)
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList = MaterialAboutList.Builder()
        .addCard(
            MaterialAboutCard.Builder()
                .apply { buildInfoItems(context).forEach { addItem(it) } }
                .build()
        )
        .addCard(
            MaterialAboutCard.Builder()
                .title(R.string.about_social_media_title)
                .apply { buildSocialMediaItems(context).forEach { addItem(it) } }
                .build()
        )
        .addCard(
            MaterialAboutCard.Builder()
                .title(R.string.about_support_title)
                .apply { buildSupportItems(context).forEach { addItem(it) } }
                .build()
        )
        .addCard(
            MaterialAboutCard.Builder()
                .title(getString(R.string.about_developer_title))
                .apply { buildDeveloperItems(context).forEach { addItem(it) } }
                .build()
        )
        .build()

    override fun shouldAnimate() = false

    private fun buildInfoItems(context: Context) = listOf(
        ConvenienceBuilder.createAppTitleItem(context),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_version_title)
            .subText(BuildConfig.VERSION_NAME)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon3.cmd_tag).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction {
                val title = getString(R.string.clipboard_title)

                requireContext().getSystemService<ClipboardManager>()?.setPrimaryClip(
                    ClipData.newPlainText(title, BuildConfig.VERSION_NAME)
                )

                requireContext().toast(R.string.clipboard_status)
            }.build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_licenses_title)
            .subText(R.string.about_licenses_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_clipboard_text).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction {
                LibsBuilder()
                    .withAutoDetect(false)
                    .withShowLoadingProgress(false)
                    .withAboutVersionShown(false)
                    .withAboutIconShown(false)
                    .withVersionShown(false)
                    .withOwnLibsActivityClass(ProxerLibsActivity::class.java)
                    .withActivityTitle(getString(R.string.about_licenses_activity_title))
                    .start(requireActivity())
            }.build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_source_code)
            .subText(R.string.about_source_code_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_code_braces).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction { showPage(repositoryLink, skipCheck = true) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_server_status)
            .subText(R.string.about_server_status_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon3.cmd_server).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction { ServerStatusActivity.navigateTo(requireActivity()) }
            .build()
    )

    private fun buildSocialMediaItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .text(R.string.about_facebook_title)
            .subText(R.string.about_facebook_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon2.cmd_facebook).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction { showPage(facebookLink, skipCheck = true) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_twitter_title)
            .subText(R.string.about_twitter_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon3.cmd_twitter).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction { showPage(twitterLink, skipCheck = true) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_youtube_title)
            .subText(R.string.about_youtube_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon3.cmd_youtube).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction { showPage(youtubeLink, skipCheck = true) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_discord_title)
            .subText(R.string.about_discord_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_discord).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction { showPage(discordLink, skipCheck = true) }
            .build()
    )

    private fun buildSupportItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon2.cmd_information).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .subText(R.string.about_support_info)
            .setOnClickAction { showPage(teamLink, skipCheck = true) }
            .build(),
        MaterialAboutActionItem.Builder()
            .text(R.string.about_support_mail_title)
            .subText(R.string.about_support_mail_description)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon.cmd_email).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")

                    putExtra(Intent.EXTRA_EMAIL, arrayOf(supportProxerMail))
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_support_mail_subject))
                }

                try {
                    startActivity(intent)
                } catch (error: ActivityNotFoundException) {
                    requireContext().toast(R.string.about_error_mail_no_activity)
                }
            }
            .build()
    )

    private fun buildDeveloperItems(context: Context) = listOf(
        MaterialAboutActionItem.Builder()
            .text(R.string.about_developer_github_title)
            .subText(developerGithubName)
            .icon(
                IconicsDrawable(context, CommunityMaterial.Icon2.cmd_github).apply {
                    colorInt = context.resolveColor(R.attr.colorIcon)
                }
            )
            .setOnClickAction {
                showPage(
                    "https://github.com/$developerGithubName".toPrefixedHttpUrl(),
                    skipCheck = true
                )
            }
            .build(),
    )
}
