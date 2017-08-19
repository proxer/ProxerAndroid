package me.proxer.app.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
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
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.MainApplication.Companion.refWatcher
import me.proxer.app.R
import me.proxer.app.chat.ChatActivity
import me.proxer.app.chat.Participant
import me.proxer.app.chat.new.NewChatActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.openHttpPage
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class AboutFragment : MaterialAboutFragment() {

    companion object {
        private val LIBRARIES = arrayOf("android_job", "android_textview_linkbuilder", "anko", "customtabshelper",
                "exomedia", "exoplayer", "flexboxlayout", "glide", "hawk", "kotterknife", "leakcanary",
                "materialbadgetextview", "materialdialogs", "materialprogressbar", "materialratingbar", "moshi",
                "okhttp", "retrofit", "rxandroid", "rxbinding", "rxlifecylce", "rxjava", "subsamplingscaleimageview",
                "tablayouthelper")

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

        fun newInstance() = AboutFragment().apply {
            arguments = bundleOf()
        }
    }

    private lateinit var customTabsHelper: CustomTabsHelperFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        refWatcher.watch(this)
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList = MaterialAboutList.Builder()
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

    override fun getTheme() = R.style.Theme_App_AboutFragment
    override fun shouldAnimate() = false

    private fun buildInfoItems() = listOf(
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
                                .withUiListener(object : LibsConfiguration.LibsUIListener {
                                    override fun preOnCreateView(view: View) = view
                                    override fun postOnCreateView(view: View) = view.apply {
                                        Utils.setNavigationBarColorIfPossible(context as Activity, R.color.primary)
                                    }
                                })
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

    private fun buildSupportItems() = listOf(
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
                        Completable
                                .fromAction {
                                    chatDao.findConferenceForUser(DEVELOPER_PROXER_NAME).let { existingConference ->
                                        when (existingConference) {
                                            null -> NewChatActivity.navigateTo(activity, false,
                                                    Participant(DEVELOPER_PROXER_NAME))
                                            else -> ChatActivity.navigateTo(activity, existingConference)
                                        }
                                    }
                                }
                                .subscribeOn(Schedulers.io())
                                .subscribe()
                    }.build()
    )

    private fun buildDeveloperItems() = listOf(
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

    private fun showPage(url: HttpUrl) = customTabsHelper.openHttpPage(activity, url)

    private fun getAboutLibrariesActivityStyle() =
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
                Configuration.UI_MODE_NIGHT_YES -> Libs.ActivityStyle.DARK
                Configuration.UI_MODE_NIGHT_UNDEFINED -> Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
                else -> throw IllegalArgumentException("Unknown mode")
            }
}
