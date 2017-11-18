package me.proxer.app.util.wrapper

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.widget.ImageView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.crossfader.Crossfader
import com.mikepenz.crossfader.view.GmailStyleCrossFadeSlidingPaneLayout
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.MiniDrawer
import com.mikepenz.materialdrawer.interfaces.ICrossfader
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import io.reactivex.subjects.PublishSubject
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.dip

/**
 * @author Ruben Gees
 */
class MaterialDrawerWrapper(
        context: Activity,
        toolbar: Toolbar,
        savedInstanceState: Bundle?,
        private val isRoot: Boolean
) {

    val itemClickSubject: PublishSubject<DrawerItem> = PublishSubject.create()
    val accountClickSubject: PublishSubject<AccountItem> = PublishSubject.create()

    val profileImageView: ImageView by unsafeLazy {
        drawer.header.findViewById<ImageView>(R.id.material_drawer_account_header_current)
    }

    val currentItem: DrawerItem
        get() = DrawerItem.fromOrNull(drawer.currentSelection)
                ?: getStickyItemIds()[drawer.currentStickyFooterSelectedPosition]

    private val header: AccountHeader
    private val drawer: Drawer

    private val miniDrawer: MiniDrawer?
    private val crossfader: Crossfader<*>?

    init {
        header = buildAccountHeader(context, savedInstanceState)
        drawer = buildDrawer(context, toolbar, header, savedInstanceState)

        miniDrawer = when (DeviceUtils.isTablet(context)) {
            true -> buildMiniDrawer(drawer)
            false -> null
        }

        crossfader = when {
            miniDrawer != null -> buildCrossfader(context, drawer, miniDrawer, savedInstanceState)
            else -> null
        }

        if (!isRoot) {
            drawer.deselect()
        }
    }

    fun onBackPressed() = when {
        crossfader?.isCrossFaded() == true -> {
            crossfader.crossFade()

            true
        }
        drawer.isDrawerOpen -> {
            drawer.closeDrawer()

            true
        }
        else -> false
    }

    fun saveInstanceState(outState: Bundle) {
        header.saveInstanceState(outState)
        drawer.saveInstanceState(outState)
        crossfader?.saveInstanceState(outState)
    }

    fun select(item: DrawerItem) {
        if (item in arrayOf(DrawerItem.INFO, DrawerItem.SETTINGS)) {
            drawer.setStickyFooterSelection(item.id, true)
        } else {
            drawer.setSelection(item.id)
            miniDrawer?.setSelection(item.id)
        }
    }

    fun refreshHeader() {
        header.profiles = generateAccountItems()
        drawer.recyclerView.adapter.notifyDataSetChanged()
        miniDrawer?.createItems()
    }

    private fun buildAccountHeader(context: Activity, savedInstanceState: Bundle?) = AccountHeaderBuilder()
            .withActivity(context)
            .withCompactStyle(true)
            .withHeaderBackground(R.color.colorPrimary)
            .withSavedInstance(savedInstanceState)
            .withProfiles(generateAccountItems())
            .withOnAccountHeaderListener { _, profile, _ -> onAccountItemClick(profile) }
            .build()

    private fun buildDrawer(
            context: Activity, toolbar: Toolbar, accountHeader: AccountHeader, savedInstanceState: Bundle?
    ) = DrawerBuilder(context)
            .withToolbar(toolbar)
            .withAccountHeader(accountHeader)
            .withDrawerItems(generateDrawerItems())
            .withStickyDrawerItems(generateStickyDrawerItems())
            .withShowDrawerOnFirstLaunch(true)
            .withTranslucentStatusBar(true)
            .withGenerateMiniDrawer(DeviceUtils.isTablet(context))
            .withSavedInstance(savedInstanceState)
            .withOnDrawerItemClickListener { _, _, item -> onDrawerItemClick(item) }
            .withOnDrawerNavigationListener { if (!isRoot) context.onBackPressed(); !isRoot }
            .let { if (DeviceUtils.isTablet(context)) it.buildView() else it.build() }
            .apply { actionBarDrawerToggle?.isDrawerIndicatorEnabled = isRoot }

    private fun buildMiniDrawer(drawer: Drawer) = drawer.miniDrawer.apply {
        withIncludeSecondaryDrawerItems(true)
    }

    private fun buildCrossfader(
            context: Activity, drawer: Drawer, miniDrawer: MiniDrawer, savedInstanceState: Bundle?
    ) = Crossfader<GmailStyleCrossFadeSlidingPaneLayout>()
            .withContent(context.findViewById(R.id.root))
            .withFirst(drawer.slider, context.dip(300))
            .withSecond(miniDrawer.build(context), context.dip(72))
            .withSavedInstance(savedInstanceState)
            .build()
            .apply {
                miniDrawer.withCrossFader(CrossfadeWrapper(this))
                getCrossFadeSlidingPaneLayout()?.setShadowResourceLeft(R.drawable.material_drawer_shadow_left)
            }

    private fun generateAccountItems() = StorageHelper.user.let {
        when (it) {
            null -> arrayListOf<IProfile<*>>(
                    ProfileDrawerItem()
                            .withName(R.string.section_guest)
                            .withIcon(R.mipmap.ic_launcher)
                            .withSelectedTextColorRes(R.color.colorAccent)
                            .withIdentifier(AccountItem.GUEST.id),
                    ProfileSettingDrawerItem()
                            .withName(R.string.section_login)
                            .withIcon(CommunityMaterial.Icon.cmd_account_key)
                            .withIdentifier(AccountItem.LOGIN.id)
            )
            else -> arrayListOf<IProfile<*>>(
                    ProfileDrawerItem()
                            .withName(it.name)
                            .withEmail(R.string.section_user_subtitle)
                            .withIcon(ProxerUrls.userImage(it.image).toString())
                            .withSelectedTextColorRes(R.color.colorAccent)
                            .withIdentifier(AccountItem.USER.id),
                    ProfileSettingDrawerItem()
                            .withName(R.string.section_notifications)
                            .withIcon(CommunityMaterial.Icon.cmd_bell_outline)
                            .withIdentifier(AccountItem.NOTIFICATIONS.id),
                    ProfileSettingDrawerItem()
                            .withName(R.string.section_ucp)
                            .withIcon(CommunityMaterial.Icon.cmd_account_key)
                            .withIdentifier(AccountItem.UCP.id),
                    ProfileSettingDrawerItem()
                            .withName(R.string.section_logout)
                            .withIcon(CommunityMaterial.Icon.cmd_account_remove)
                            .withIdentifier(AccountItem.LOGOUT.id)
            )
        }
    }

    private fun generateDrawerItems() = arrayListOf<IDrawerItem<*, *>>(
            PrimaryDrawerItem()
                    .withName(R.string.section_news)
                    .withIcon(CommunityMaterial.Icon.cmd_newspaper)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withIdentifier(DrawerItem.NEWS.id),
            PrimaryDrawerItem()
                    .withName(R.string.section_chat)
                    .withIcon(CommunityMaterial.Icon.cmd_message_text)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withIdentifier(DrawerItem.CHAT.id),
            PrimaryDrawerItem()
                    .withName(R.string.section_bookmarks)
                    .withIcon(CommunityMaterial.Icon.cmd_bookmark)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withIdentifier(DrawerItem.BOOKMARKS.id),
            PrimaryDrawerItem()
                    .withName(R.string.section_anime)
                    .withIcon(CommunityMaterial.Icon.cmd_television)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withIdentifier(DrawerItem.ANIME.id),
            PrimaryDrawerItem()
                    .withName(R.string.section_manga)
                    .withIcon(CommunityMaterial.Icon.cmd_book_open_page_variant)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withIdentifier(DrawerItem.MANGA.id),
            PrimaryDrawerItem()
                    .withName(R.string.section_local_manga)
                    .withIcon(CommunityMaterial.Icon.cmd_cloud_download)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withIdentifier(DrawerItem.LOCAL_MANGA.id)
    )

    private fun generateStickyDrawerItems() = arrayListOf<IDrawerItem<*, *>>(
            PrimaryDrawerItem()
                    .withName(R.string.section_info)
                    .withIcon(CommunityMaterial.Icon.cmd_information_outline)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withIdentifier(DrawerItem.INFO.id),
            PrimaryDrawerItem()
                    .withName(R.string.section_settings)
                    .withIcon(CommunityMaterial.Icon.cmd_settings)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectable(isRoot)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withIdentifier(DrawerItem.SETTINGS.id)
    ).apply {
        @Suppress("ConstantConditionIf")
        if (!BuildConfig.STORE) {
            add(1, PrimaryDrawerItem()
                    .withName(R.string.section_donate)
                    .withIcon(CommunityMaterial.Icon.cmd_gift)
                    .withSelectedTextColorRes(R.color.colorAccent)
                    .withSelectedIconColorRes(R.color.colorAccent)
                    .withSelectable(false)
                    .withIdentifier(DrawerItem.DONATE.id))
        }
    }

    private fun onDrawerItemClick(item: IDrawerItem<*, *>) = DrawerItem.fromOrDefault(item.identifier).let {
        if (it in getStickyItemIds()) {
            miniDrawer?.adapter?.deselect()

            if (!it.shouldKeepOpen) crossfader?.crossFade()
        }

        itemClickSubject.onNext(it)

        it.shouldKeepOpen
    }

    private fun onAccountItemClick(profile: IProfile<*>) = AccountItem.fromOrDefault(profile.identifier).let {
        accountClickSubject.onNext(it)

        it.shouldKeepOpen
    }

    private fun getStickyItemIds() = DrawerItem.values().filter { it.id >= 10L }

    enum class DrawerItem(val id: Long, val shouldKeepOpen: Boolean) {
        NEWS(0L, false),
        CHAT(1L, false),
        BOOKMARKS(2L, false),
        ANIME(3L, false),
        MANGA(4L, false),
        LOCAL_MANGA(5L, false),
        INFO(10L, false),
        DONATE(11L, true),
        SETTINGS(12L, false);

        companion object {
            fun fromOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromOrDefault(id: Long?) = fromOrNull(id) ?: NEWS
        }
    }

    enum class AccountItem(val id: Long, val shouldKeepOpen: Boolean) {
        GUEST(100L, true),
        LOGIN(101L, true),
        USER(102L, true),
        LOGOUT(103L, true),
        NOTIFICATIONS(104L, true),
        UCP(105L, true);

        companion object {
            fun fromOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromOrDefault(id: Long?) = fromOrNull(id) ?: USER
        }
    }

    private class CrossfadeWrapper(private val crossfader: Crossfader<*>) : ICrossfader {
        override fun crossfade() = crossfader.crossFade()
        override fun isCrossfaded() = crossfader.isCrossFaded()
    }
}
