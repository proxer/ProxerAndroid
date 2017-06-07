package me.proxer.app.fragment

import android.content.Context
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.activity.ChatActivity
import me.proxer.app.activity.NewChatActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.application.MainApplication
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.Participant
import me.proxer.app.util.extension.openHttpPage
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync

/**
 * @author Ruben Gees
 */
class AboutFragment : MaterialAboutFragment() {

    companion object {
        private val LIBRARIES = arrayOf("glide", "hawk", "materialdialogs",
                "eventbus", "okhttp", "leakcanary", "anko", "moshi",
                "android_textview_linkbuilder", "kotterknife")
        private val EXCLUDED_LIBRARIES = arrayOf("fastadapter", "materialize")

        private val REPOSITORY_LINK = HttpUrl.Builder()
                .scheme("https")
                .host("github.com")
                .addPathSegment("proxer")
                .addPathSegment("ProxerAndroid")
                .build()
        private val SUPPORT_LINK = ProxerUrls.forumWeb("anwendungen",
                "374605-opensource-android-proxer-app-fuer-android", Device.MOBILE)

        private const val DEVELOPER_PROXER_NAME = "RubyGee"
        private const val DEVELOPER_PROXER_ID = "121658"
        private const val DEVELOPER_GITHUB_NAME = "rubengees"

        fun newInstance(): AboutFragment {
            return AboutFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    private lateinit var customTabsHelper: CustomTabsHelperFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        return MaterialAboutList.Builder()
                .addCard(MaterialAboutCard.Builder()
                        .apply { buildInfoItems().forEach { addItem(it) } }
                        .build())
                .addCard(MaterialAboutCard.Builder()
                        .title(R.string.about_support_title)
                        .apply { buildSupportItems().forEach { addItem(it) } }
                        .build())
                .addCard(MaterialAboutCard.Builder()
                        .title(getString(R.string.about_developer_title))
                        .apply { buildDeveloperItems().forEach { addItem(it) } }
                        .build())
                .build()
    }

    override fun getTheme(): Int {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> THEME_LIGHT
            Configuration.UI_MODE_NIGHT_YES -> THEME_DARK
            Configuration.UI_MODE_NIGHT_UNDEFINED -> THEME_LIGHT
            else -> throw RuntimeException("Unknown mode")
        }
    }

    override fun shouldAnimate() = false

    private fun buildInfoItems(): List<MaterialAboutItem> {
        return listOf(
                ConvenienceBuilder.createAppTitleItem(context),
                ConvenienceBuilder.createVersionActionItem(context,
                        IconicsDrawable(context, CommunityMaterial.Icon.cmd_tag)
                                .colorRes(R.color.icon),
                        getString(R.string.about_info_version_title), false),
                MaterialAboutActionItem.Builder()
                        .text(R.string.about_info_licenses_title)
                        .subText(R.string.about_info_licenses_description)
                        .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_clipboard_text)
                                .colorRes(R.color.icon))
                        .setOnClickAction {
                            LibsBuilder().withAutoDetect(false)
                                    .withShowLoadingProgress(false)
                                    .withAboutVersionShown(false)
                                    .withAboutIconShown(false)
                                    .withAboutDescription(getString(R.string.about_info_licenses_activity_description))
                                    .withLibraries(*LIBRARIES)
                                    .withExcludedLibraries(*EXCLUDED_LIBRARIES)
                                    .withFields(R.string::class.java.fields)
                                    .withActivityStyle(getAboutLibrariesActivityStyle())
                                    .withActivityTitle(getString(R.string.about_info_licenses_activity_title))
                                    .start(context)
                        }.build(),
                MaterialAboutActionItem.Builder()
                        .text(R.string.about_info_source_code)
                        .subText(R.string.about_info_source_code_description)
                        .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_code_braces)
                                .colorRes(R.color.icon))
                        .setOnClickAction {
                            showPage(REPOSITORY_LINK)
                        }.build()
        )
    }

    private fun buildSupportItems(): List<MaterialAboutItem> {
        return listOf(
                MaterialAboutActionItem.Builder()
                        .text(R.string.about_support_forum_title)
                        .subText(R.string.about_support_forum_description)
                        .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_forum)
                                .colorRes(R.color.icon))
                        .setOnClickAction {
                            showPage(SUPPORT_LINK)
                        }.build(),
                MaterialAboutActionItem.Builder()
                        .text(R.string.about_support_message_title)
                        .subText(R.string.about_support_message_description)
                        .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_email)
                                .colorRes(R.color.icon))
                        .setOnClickAction {
                            doAsync {
                                val existingChat = chatDb.findConferenceForUser(DEVELOPER_PROXER_NAME)

                                when (existingChat) {
                                    null -> {
                                        NewChatActivity.navigateTo(activity, false, Participant(DEVELOPER_PROXER_NAME))
                                    }
                                    else -> weakRef.get()?.let { ChatActivity.navigateTo(it.activity, existingChat) }
                                }
                            }
                        }.build()
        )
    }

    private fun buildDeveloperItems(): List<MaterialAboutItem> {
        return listOf(
                MaterialAboutActionItem.Builder()
                        .text(R.string.about_developer_github_title)
                        .subText(DEVELOPER_GITHUB_NAME)
                        .icon(IconicsDrawable(context, CommunityMaterial.Icon.cmd_github_circle)
                                .colorRes(R.color.icon))
                        .setOnClickAction {
                            showPage(HttpUrl.parse("https://github.com/$DEVELOPER_GITHUB_NAME")
                                    ?: throw NullPointerException())
                        }.build(),
                MaterialAboutActionItem.Builder()
                        .text(getString(R.string.about_developer_proxer_title))
                        .subText(DEVELOPER_PROXER_NAME)
                        .icon(ContextCompat.getDrawable(context, R.drawable.ic_stat_proxer).apply {
                            setColorFilter(ContextCompat.getColor(context, R.color.icon), PorterDuff.Mode.SRC_IN)
                        })
                        .setOnClickAction {
                            ProfileActivity.navigateTo(activity, DEVELOPER_PROXER_ID, DEVELOPER_PROXER_NAME, null)
                        }.build()
        )
    }

    private fun showPage(url: HttpUrl) {
        customTabsHelper.openHttpPage(activity, url)
    }

    private fun getAboutLibrariesActivityStyle(): Libs.ActivityStyle {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
            Configuration.UI_MODE_NIGHT_YES -> Libs.ActivityStyle.DARK
            Configuration.UI_MODE_NIGHT_UNDEFINED -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
            else -> throw RuntimeException("Unknown mode")
        }
    }
}
