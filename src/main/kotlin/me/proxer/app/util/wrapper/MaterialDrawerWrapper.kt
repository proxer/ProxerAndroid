package me.proxer.app.util.wrapper

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.mikepenz.crossfader.Crossfader
import com.mikepenz.crossfader.view.GmailStyleCrossFadeSlidingPaneLayout
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
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
import me.proxer.app.R
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.backgroundColorAttr
import me.proxer.app.util.extension.colorAttr
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.safeInject
import me.proxer.library.util.ProxerUrls
import org.koin.core.KoinComponent

/**
 * @author Ruben Gees
 */
class MaterialDrawerWrapper(
    context: Activity,
    toolbar: Toolbar,
    savedInstanceState: Bundle?,
    private val isRoot: Boolean,
    private val isMain: Boolean
) : KoinComponent {

    val itemClickSubject: PublishSubject<DrawerItem> = PublishSubject.create()
    val accountClickSubject: PublishSubject<AccountItem> = PublishSubject.create()

    val currentItem: DrawerItem?
        get() {
            val idToUse = when {
                drawer.currentSelection >= 0 -> drawer.currentSelection
                else -> drawer.currentStickyFooterSelectedPosition + 10L
            }

            return DrawerItem.fromIdOrNull(idToUse)
        }

    private val storageHelper by safeInject<StorageHelper>()

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
            miniDrawer?.setSelection(-1)
        }
    }

    fun onBackPressed() = when {
        header.isSelectionListShown -> {
            header.toggleSelectionList(header.view.context)

            // Return true (handled) if the drawer is open.
            // Otherwise return false to let the caller handle the back press.
            crossfader?.isCrossFaded == true || drawer.isDrawerOpen
        }
        crossfader?.isCrossFaded == true -> {
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

    fun select(item: DrawerItem, fireOnClick: Boolean = true) {
        if (item.id >= 10) {
            drawer.setStickyFooterSelection(item.id, fireOnClick)
        } else {
            drawer.setSelection(item.id, fireOnClick)
            miniDrawer?.setSelection(item.id)
        }
    }

    fun refreshHeader(context: Context) {
        val oldProfiles = header.profiles
        val newProfiles = generateAccountItems(context)

        if (
            oldProfiles == null ||
            oldProfiles.size != newProfiles.size ||
            oldProfiles.withIndex().any { (index, item) ->
                item.identifier != newProfiles[index].identifier ||
                    item.name?.text != newProfiles[index].name?.text
            }
        ) {
            header.profiles = generateAccountItems(context)
            drawer.recyclerView.adapter?.notifyDataSetChanged()
            miniDrawer?.createItems()
        }
    }

    fun disableSelectivity() {
        drawer.drawerItems.forEach { it.isSelectable = false }
    }

    private fun buildAccountHeader(context: Activity, savedInstanceState: Bundle?) = AccountHeaderBuilder()
        .withActivity(context)
        .withCompactStyle(true)
        .withHeaderBackground(ColorDrawable(context.resolveColor(R.attr.colorPrimary)))
        .withTextColor(context.resolveColor(R.attr.colorOnPrimary))
        .withSavedInstance(savedInstanceState)
        .withProfiles(generateAccountItems(context))
        .withOnAccountHeaderListener(object : AccountHeader.OnAccountHeaderListener {
            override fun onProfileChanged(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
                return onAccountItemClick(profile)
            }
        })
        .build()

    private fun buildDrawer(
        context: Activity,
        toolbar: Toolbar,
        accountHeader: AccountHeader,
        savedInstanceState: Bundle?
    ) = DrawerBuilder(context)
        .withToolbar(toolbar)
        .withAccountHeader(accountHeader)
        .withDrawerItems(generateDrawerItems())
        .withStickyDrawerItems(generateStickyDrawerItems())
        .withShowDrawerOnFirstLaunch(true)
        .withTranslucentStatusBar(true)
        .withGenerateMiniDrawer(DeviceUtils.isTablet(context))
        .withSavedInstance(savedInstanceState)
        .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
            override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                return onDrawerItemClick(drawerItem)
            }
        })
        .withOnDrawerNavigationListener(object : Drawer.OnDrawerNavigationListener {
            override fun onNavigationClickListener(clickedView: View): Boolean {
                if (!isRoot) context.onBackPressed()

                return !isRoot
            }
        })
        .let { if (DeviceUtils.isTablet(context)) it.buildView() else it.build() }
        .apply { actionBarDrawerToggle?.isDrawerIndicatorEnabled = isRoot }

    private fun buildMiniDrawer(drawer: Drawer) = drawer.miniDrawer?.apply {
        withIncludeSecondaryDrawerItems(true)
    }

    private fun buildCrossfader(
        context: Activity,
        drawer: Drawer,
        miniDrawer: MiniDrawer,
        savedInstanceState: Bundle?
    ) = Crossfader<GmailStyleCrossFadeSlidingPaneLayout>()
        .withContent(context.findViewById(R.id.root))
        .withFirst(drawer.slider, context.dip(300))
        .withSecond(miniDrawer.build(context), context.dip(72))
        .withSavedInstance(savedInstanceState)
        .withGmailStyleSwiping()
        .build()
        .apply {
            miniDrawer.withCrossFader(CrossfaderWrapper(this))
            getCrossFadeSlidingPaneLayout()?.setShadowResourceLeft(R.drawable.material_drawer_shadow_left)
        }

    private fun onDrawerItemClick(item: IDrawerItem<*>) = DrawerItem.fromIdOrDefault(item.identifier).let {
        if (it.id >= 10L) {
            miniDrawer?.setSelection(-1)
        }

        itemClickSubject.onNext(it)

        false
    }

    private fun onAccountItemClick(profile: IProfile<*>) = AccountItem.fromIdOrDefault(profile.identifier).let {
        accountClickSubject.onNext(it)

        // Workaround for crash in current MaterialDrawer version.
        when {
            crossfader?.isCrossFaded == true -> {
                crossfader.crossFade()

                true
            }
            drawer.isDrawerOpen -> {
                drawer.closeDrawer()

                true
            }
            else -> false
        }
    }

    private fun generateAccountItems(context: Context): MutableList<IProfile<*>> = storageHelper.user.let {
        when (it) {
            null -> mutableListOf(
                ProfileDrawerItem()
                    .withName(R.string.section_guest)
                    .withIcon(R.mipmap.ic_launcher)
                    .withIdentifier(AccountItem.GUEST.id),
                ProfileSettingDrawerItem()
                    .withName(R.string.section_login)
                    .withIcon(CommunityMaterial.Icon.cmd_account_key)
                    .withIdentifier(AccountItem.LOGIN.id)
            )
            else -> mutableListOf(
                ProfileDrawerItem()
                    .withName(it.name)
                    .withEmail(R.string.section_user_subtitle)
                    .apply {
                        if (it.image.isBlank()) {
                            withIcon(
                                IconicsDrawable(context, CommunityMaterial.Icon.cmd_account)
                                    .sizeDp(48)
                                    .colorAttr(context, R.attr.colorOnPrimary)
                                    .backgroundColorAttr(context, R.attr.colorPrimary)
                            )
                        } else {
                            withIcon(ProxerUrls.userImage(it.image).toString())
                        }
                    }
                    .withIdentifier(AccountItem.USER.id),
                ProfileSettingDrawerItem()
                    .withName(R.string.section_notifications)
                    .withIcon(CommunityMaterial.Icon.cmd_bell_outline)
                    .withIdentifier(AccountItem.NOTIFICATIONS.id),
                ProfileSettingDrawerItem()
                    .withName(R.string.section_profile_settings)
                    .withIcon(CommunityMaterial.Icon.cmd_account_settings)
                    .withIdentifier(AccountItem.PROFILE_SETTINGS.id),
                ProfileSettingDrawerItem()
                    .withName(R.string.section_logout)
                    .withIcon(CommunityMaterial.Icon.cmd_account_remove)
                    .withIdentifier(AccountItem.LOGOUT.id)
            )
        }
    }

    private fun generateDrawerItems() = listOf(
        PrimaryDrawerItem()
            .withName(R.string.section_news)
            .withIcon(CommunityMaterial.Icon2.cmd_newspaper)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.NEWS.id),
        PrimaryDrawerItem()
            .withName(R.string.section_chat)
            .withIcon(CommunityMaterial.Icon2.cmd_message_text)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.CHAT.id),
        PrimaryDrawerItem()
            .withName(R.string.section_bookmarks)
            .withIcon(CommunityMaterial.Icon.cmd_bookmark)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.BOOKMARKS.id),
        PrimaryDrawerItem()
            .withName(R.string.section_anime)
            .withIcon(CommunityMaterial.Icon2.cmd_television)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.ANIME.id),
        PrimaryDrawerItem()
            .withName(R.string.section_schedule)
            .withIcon(CommunityMaterial.Icon.cmd_calendar)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.SCHEDULE.id),
        PrimaryDrawerItem()
            .withName(R.string.section_manga)
            .withIcon(CommunityMaterial.Icon.cmd_book_open_page_variant)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.MANGA.id)
    )

    private fun generateStickyDrawerItems(): MutableList<IDrawerItem<*>> = mutableListOf(
        PrimaryDrawerItem()
            .withName(R.string.section_info)
            .withIcon(CommunityMaterial.Icon2.cmd_information_outline)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.INFO.id),
        PrimaryDrawerItem()
            .withName(R.string.section_settings)
            .withIcon(CommunityMaterial.Icon2.cmd_settings)
            .withSelectable(isMain)
            .withIdentifier(DrawerItem.SETTINGS.id)
    )

    enum class DrawerItem(val id: Long) {
        NEWS(0L),
        CHAT(1L),
        MESSENGER(1L),
        BOOKMARKS(2L),
        ANIME(3L),
        SCHEDULE(4L),
        MANGA(5L),
        INFO(10L),
        SETTINGS(11L);

        companion object {
            fun fromIdOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromIdOrDefault(id: Long?) = fromIdOrNull(id) ?: NEWS
        }
    }

    enum class AccountItem(val id: Long) {
        GUEST(100L),
        LOGIN(101L),
        USER(102L),
        LOGOUT(103L),
        NOTIFICATIONS(104L),
        PROFILE_SETTINGS(106L);

        companion object {
            fun fromIdOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromIdOrDefault(id: Long?) = fromIdOrNull(id) ?: USER
        }
    }

    private class CrossfaderWrapper(private val crossfader: Crossfader<*>) : ICrossfader {
        override val isCrossfaded get() = crossfader.isCrossFaded
        override fun crossfade() = crossfader.crossFade()
    }
}
