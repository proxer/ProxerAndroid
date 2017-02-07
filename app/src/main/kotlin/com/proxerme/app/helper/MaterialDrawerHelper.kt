package com.proxerme.app.helper

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.proxerme.app.R
import com.proxerme.app.manager.UserManager
import com.proxerme.library.info.ProxerUrlHolder
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */

class MaterialDrawerHelper(context: Activity, toolbar: Toolbar,
                           savedInstanceState: Bundle?,
                           private val itemClickCallback: (id: DrawerItem) -> Boolean = { false },
                           private val accountClickCallback: (id: AccountItem) -> Boolean = { false }) {

    companion object {
        private const val STATE_CURRENT_DRAWER_ITEM_ID = "material_drawer_helper_current_id"
    }

    private val header: AccountHeader
    private val drawer: Drawer

    private var currentItem: DrawerItem? = null

    init {
        header = buildAccountHeader(context, savedInstanceState)
        drawer = buildDrawer(context, toolbar, header, savedInstanceState)
        currentItem = DrawerItem.fromOrNull(savedInstanceState?.getLong(STATE_CURRENT_DRAWER_ITEM_ID))
    }

    fun onBackPressed(): Boolean {
        if (isDrawerOpen()) {
            drawer.closeDrawer()

            return true
        } else if (currentItem != DrawerItem.NEWS) {
            select(DrawerItem.NEWS)

            return true
        } else {
            return false
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putLong(STATE_CURRENT_DRAWER_ITEM_ID, currentItem?.id ?: DrawerItem.NEWS.id)

        header.saveInstanceState(outState)
        drawer.saveInstanceState(outState)
    }

    fun isDrawerOpen(): Boolean {
        return drawer.isDrawerOpen
    }

    fun select(item: DrawerItem) {
        drawer.setSelection(item.id)
    }

    fun refreshHeader(context: Activity) {
        header.profiles = generateAccountItems(context)
        drawer.recyclerView.adapter.notifyDataSetChanged()
    }

    private fun buildAccountHeader(context: Activity, savedInstanceState: Bundle?): AccountHeader {
        return AccountHeaderBuilder()
                .withActivity(context)
                .withCompactStyle(true)
                .withHeaderBackground(R.color.colorPrimary)
                .withOnAccountHeaderListener { view, profile, current ->
                    onAccountItemClick(view, profile, current)
                }
                .withSavedInstance(savedInstanceState)
                .withProfiles(generateAccountItems(context))
                .build()
    }

    private fun generateAccountItems(context: Activity): List<IProfile<*>> {
        val user = UserManager.user

        if (user == null) {
            return arrayListOf(
                    ProfileDrawerItem()
                            .withName(context.getString(R.string.drawer_account_guest))
                            .withIcon(R.mipmap.ic_launcher)
                            .withSelectedTextColorRes(R.color.colorAccent)
                            .withIdentifier(AccountItem.GUEST.id),
                    ProfileSettingDrawerItem()
                            .withName(context.getString(R.string.drawer_account_login))
                            .withIcon(CommunityMaterial.Icon.cmd_account_key)
                            .withIconTinted(true)
                            .withIdentifier(AccountItem.LOGIN.id))
        } else {
            return arrayListOf(
                    ProfileDrawerItem()
                            .withName(user.username)
                            .withEmail(getTextForLoginState(context))
                            .withIcon(ProxerUrlHolder.getUserImageUrl(user.imageId).toString())
                            .withSelectedTextColorRes(R.color.colorAccent)
                            .withIdentifier(AccountItem.USER.id),
                    ProfileSettingDrawerItem()
                            .withName(context.getString(R.string.drawer_account_ucp))
                            .withIcon(CommunityMaterial.Icon.cmd_account_key)
                            .withIconTinted(true)
                            .withIdentifier(AccountItem.UCP.id),
                    ProfileSettingDrawerItem()
                            .withName(context.getString(R.string.drawer_account_logout))
                            .withIcon(CommunityMaterial.Icon.cmd_account_remove)
                            .withIconTinted(true)
                            .withIdentifier(AccountItem.LOGOUT.id)
            )
        }
    }

    private fun buildDrawer(context: Activity, toolbar: Toolbar, accountHeader: AccountHeader,
                            savedInstanceState: Bundle?): Drawer {
        return DrawerBuilder(context)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader)
                .withDrawerItems(generateDrawerItems())
                .withStickyDrawerItems(generateStickyDrawerItems())
                .withOnDrawerItemClickListener { view, id, item ->
                    onDrawerItemClick(view, id, item)
                }
                .withShowDrawerOnFirstLaunch(true)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(true)
                .withSavedInstance(savedInstanceState)
                .build()
    }

    private fun generateDrawerItems(): List<IDrawerItem<*, *>> {
        return arrayListOf(
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_news)
                        .withIcon(CommunityMaterial.Icon.cmd_newspaper)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.NEWS.id),
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_chat)
                        .withIcon(CommunityMaterial.Icon.cmd_message_text)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.CHAT.id),
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_reminder)
                        .withIcon(CommunityMaterial.Icon.cmd_bookmark)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.REMINDER.id),
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_anime)
                        .withIcon(CommunityMaterial.Icon.cmd_television)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.ANIME.id),
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_manga)
                        .withIcon(CommunityMaterial.Icon.cmd_book_open_variant)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withBadgeStyle(BadgeStyle()
                                .withColorRes(R.color.colorAccent)
                                .withTextColorRes(android.R.color.white))
                        .withIdentifier(DrawerItem.MANGA.id)
        )
    }

    private fun generateStickyDrawerItems(): ArrayList<IDrawerItem<*, *>> {
        return arrayListOf(
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_info)
                        .withIcon(CommunityMaterial.Icon.cmd_information_outline)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withIdentifier(DrawerItem.INFO.id),
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_donate)
                        .withIcon(CommunityMaterial.Icon.cmd_gift)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withSelectable(false)
                        .withIdentifier(DrawerItem.DONATE.id),
                PrimaryDrawerItem()
                        .withName(R.string.drawer_item_settings)
                        .withIcon(CommunityMaterial.Icon.cmd_settings)
                        .withSelectedTextColorRes(R.color.colorAccent)
                        .withSelectedIconColorRes(R.color.colorAccent)
                        .withIdentifier(DrawerItem.SETTINGS.id))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDrawerItemClick(view: View?, id: Int, item: IDrawerItem<*, *>): Boolean {
        if (item.identifier != currentItem?.id) {
            val newItem = DrawerItem.fromOrDefault(item.identifier)

            if (item.isSelectable) {
                currentItem = newItem
            }

            return itemClickCallback.invoke(newItem)
        }

        return true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAccountItemClick(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
        return accountClickCallback.invoke(AccountItem.fromOrDefault(profile.identifier))
    }

    private fun getTextForLoginState(context: Context): String {
        return when (UserManager.ongoingState) {
            UserManager.OngoingState.LOGGING_IN -> {
                context.getString(R.string.login_state_indicator_logging_in)
            }

            UserManager.OngoingState.LOGGING_OUT -> {
                context.getString(R.string.login_state_indicator_logging_out)
            }

            UserManager.OngoingState.NONE -> when (UserManager.loginState) {
                UserManager.LoginState.LOGGED_IN -> {
                    context.getString(R.string.login_state_indicator_logged_in)
                }

                UserManager.LoginState.LOGGED_OUT -> {
                    context.getString(R.string.login_state_indicator_logged_out)
                }
            }
        }
    }

    enum class DrawerItem(val id: Long) {
        NEWS(0L),
        CHAT(1L),
        REMINDER(2L),
        ANIME(3L),
        MANGA(4L),
        INFO(10L),
        DONATE(11L),
        SETTINGS(12L);

        companion object {
            fun fromOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromOrDefault(id: Long?) = fromOrNull(id) ?: NEWS
        }
    }

    enum class AccountItem(val id: Long) {
        GUEST(100L),
        LOGIN(101L),
        USER(102L),
        LOGOUT(103L),
        UCP(104L);

        companion object {
            fun fromOrNull(id: Long?) = values().firstOrNull { it.id == id }
            fun fromOrDefault(id: Long?) = fromOrNull(id) ?: USER
        }
    }
}
