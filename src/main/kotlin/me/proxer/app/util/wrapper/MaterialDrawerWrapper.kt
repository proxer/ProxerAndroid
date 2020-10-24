package me.proxer.app.util.wrapper

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.mikepenz.crossfader.Crossfader
import com.mikepenz.crossfader.view.CrossFadeSlidingPaneLayout
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.backgroundColorInt
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.interfaces.ICrossfader
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.descriptionRes
import com.mikepenz.materialdrawer.model.interfaces.iconDrawable
import com.mikepenz.materialdrawer.model.interfaces.iconRes
import com.mikepenz.materialdrawer.model.interfaces.iconUrl
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.addStickyDrawerItems
import com.mikepenz.materialdrawer.util.getStickyFooterPositionByIdentifier
import com.mikepenz.materialdrawer.util.setStickyFooterSelection
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import com.mikepenz.materialdrawer.widget.MiniDrawerSliderView
import io.reactivex.subjects.PublishSubject
import me.proxer.app.R
import me.proxer.app.ui.view.CrossfadingDrawerLayout
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.safeInject
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
@Suppress("UNUSED_PARAMETER")
class MaterialDrawerWrapper(
    context: Activity,
    toolbar: Toolbar,
    savedInstanceState: Bundle?,
    private val isMain: Boolean
) {

    val itemClickSubject: PublishSubject<DrawerItem> = PublishSubject.create()
    val profileClickSubject: PublishSubject<ProfileItem> = PublishSubject.create()

    val currentItem: DrawerItem?
        get() {
            val selection = sliderView.selectExtension.selectedItems.firstOrNull()?.identifier

            return when {
                selection != null -> DrawerItem.fromIdOrNull(selection)
                else -> null
            }
        }

    private val storageHelper by safeInject<StorageHelper>()

    private val drawerLayout: CrossfadingDrawerLayout
    private val sliderView: MaterialDrawerSliderView
    private val headerView: AccountHeaderView

    private val miniSliderView: MiniDrawerSliderView?
    private val crossfader: Crossfader<*>?

    init {
        val isTablet = DeviceUtils.isTablet(context)

        val sliderViewToUse = when (isTablet) {
            true -> MaterialDrawerSliderView(context)
            false -> context.findViewById(R.id.slider)
        }

        val sliderView = sliderViewToUse.apply {
            addItems(*generateDrawerItems())
            addStickyDrawerItems(*generateStickyDrawerItems())
            setSavedInstance(savedInstanceState)

            onDrawerItemClickListener = { _, item, _ ->
                val drawerItem = DrawerItem.fromIdOrDefault(item.identifier)

                if (drawerItem.id >= 10L) {
                    miniDrawer?.setSelection(-1)
                }

                itemClickSubject.onNext(drawerItem)

                false
            }
        }

        val headerView = AccountHeaderView(context).apply {
            attachToSliderView(sliderView)
            addProfiles(*generateProfiles(context))
            withSavedInstance(savedInstanceState)

            headerBackground = ImageHolder(ColorDrawable(context.resolveColor(R.attr.colorPrimary)))

            onAccountHeaderListener = { _, item, _ ->
                val drawerItem = ProfileItem.fromIdOrDefault(item.identifier)

                profileClickSubject.onNext(drawerItem)

                false
            }
        }

        this.drawerLayout = context.findViewById(R.id.root)
        this.sliderView = sliderView
        this.headerView = headerView

        if (isTablet) {
            val miniSliderView = MiniDrawerSliderView(context).apply {
                drawer = sliderView
            }

            val crossfader = Crossfader<CrossFadeSlidingPaneLayout>()
                .withContent(context.findViewById(R.id.innerRoot))
                .withFirst(sliderView, context.dip(320))
                .withSecond(miniSliderView, context.dip(72))
                .withSavedInstance(savedInstanceState)
                .withGmailStyleSwiping()
                .build()
                .apply { getCrossFadeSlidingPaneLayout().setShadowResourceLeft(R.drawable.material_drawer_shadow_left) }

            miniSliderView.crossFader = CrossfaderWrapper(crossfader)
            drawerLayout.crossfader = crossfader

            this.miniSliderView = miniSliderView
            this.crossfader = crossfader
        } else {
            this.miniSliderView = null
            this.crossfader = null
        }
    }

    fun onBackPressed() = when {
        headerView.selectionListShown -> {
            headerView.selectionListShown = false

            // Return true (handled) if the drawer is open.
            // Otherwise return false to let the caller handle the back press.
            crossfader?.isCrossFaded == true || sliderView.drawerLayout?.isOpen == true
        }
        crossfader?.isCrossFaded == true -> {
            crossfader.crossFade()

            true
        }
        sliderView.drawerLayout?.isOpen == true -> {
            sliderView.drawerLayout?.close()

            true
        }
        else -> false
    }

    fun saveInstanceState(outState: Bundle) {
        sliderView.saveInstanceState(outState)
        headerView.saveInstanceState(outState)
        crossfader?.saveInstanceState(outState)
    }

    fun select(item: DrawerItem, fireOnClick: Boolean = true) {
        if (item.id >= 10) {
            sliderView.setStickyFooterSelection(sliderView.getStickyFooterPositionByIdentifier(item.id), fireOnClick)
        } else {
            sliderView.setSelection(item.id, fireOnClick)
            miniSliderView?.setSelection(item.id)
        }
    }

    fun refreshHeader(context: Context) {
        headerView.profiles = generateProfiles(context).toMutableList()
    }

    private fun generateProfiles(context: Context): Array<IProfile> = storageHelper.user.let {
        when (it) {
            null -> arrayOf(
                ProfileDrawerItem().apply {
                    nameRes = R.string.section_guest
                    iconRes = R.mipmap.ic_launcher
                    identifier = ProfileItem.GUEST.id
                },
                ProfileSettingDrawerItem().apply {
                    nameRes = R.string.section_login
                    iconicsIcon = CommunityMaterial.Icon.cmd_account_key
                    identifier = ProfileItem.LOGIN.id
                }
            )
            else -> arrayOf(
                ProfileDrawerItem().apply {
                    nameText = it.name
                    descriptionRes = R.string.section_user_subtitle
                    identifier = ProfileItem.USER.id

                    if (it.image.isBlank()) {
                        iconDrawable = IconicsDrawable(context, CommunityMaterial.Icon.cmd_account).apply {
                            backgroundColorInt = context.resolveColor(R.attr.colorPrimary)
                            colorInt = context.resolveColor(R.attr.colorOnPrimary)
                            sizeDp = 48
                        }
                    } else {
                        iconUrl = ProxerUrls.userImage(it.image).toString()
                    }
                },
                ProfileSettingDrawerItem().apply {
                    nameRes = R.string.section_notifications
                    iconicsIcon = CommunityMaterial.Icon.cmd_bell_outline
                    identifier = ProfileItem.NOTIFICATIONS.id
                },
                ProfileSettingDrawerItem().apply {
                    nameRes = R.string.section_profile_settings
                    iconicsIcon = CommunityMaterial.Icon.cmd_account_settings
                    identifier = ProfileItem.PROFILE_SETTINGS.id
                },
                ProfileSettingDrawerItem().apply {
                    nameRes = R.string.section_logout
                    iconicsIcon = CommunityMaterial.Icon.cmd_account_remove
                    identifier = ProfileItem.LOGOUT.id
                }
            )
        }
    }

    private fun generateDrawerItems() = arrayOf(
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_news
            iconicsIcon = CommunityMaterial.Icon3.cmd_newspaper
            identifier = DrawerItem.NEWS.id
            isSelectable = isMain
        },
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_chat
            iconicsIcon = CommunityMaterial.Icon3.cmd_message_text
            identifier = DrawerItem.CHAT.id
            isSelectable = isMain
        },
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_bookmarks
            iconicsIcon = CommunityMaterial.Icon.cmd_bookmark
            identifier = DrawerItem.BOOKMARKS.id
            isSelectable = isMain
        },
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_anime
            iconicsIcon = CommunityMaterial.Icon3.cmd_television
            identifier = DrawerItem.ANIME.id
            isSelectable = isMain
        },
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_schedule
            iconicsIcon = CommunityMaterial.Icon.cmd_calendar
            identifier = DrawerItem.SCHEDULE.id
            isSelectable = isMain
        },
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_manga
            iconicsIcon = CommunityMaterial.Icon.cmd_book_open_page_variant
            identifier = DrawerItem.MANGA.id
            isSelectable = isMain
        }
    )

    private fun generateStickyDrawerItems() = arrayOf(
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_info
            iconicsIcon = CommunityMaterial.Icon2.cmd_information_outline
            isSelectable = isMain
            identifier = DrawerItem.INFO.id
        },
        PrimaryDrawerItem().apply {
            nameRes = R.string.section_settings
            iconicsIcon = CommunityMaterial.Icon.cmd_cog
            isSelectable = isMain
            identifier = DrawerItem.SETTINGS.id
        }
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

    enum class ProfileItem(val id: Long) {
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
