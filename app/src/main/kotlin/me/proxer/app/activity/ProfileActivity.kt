package me.proxer.app.activity

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.IconicsMenuInflatorUtil
import me.proxer.app.R
import me.proxer.app.activity.base.ImageTabsActivity
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.entity.chat.Participant
import me.proxer.app.fragment.profile.ProfileFragment
import me.proxer.app.fragment.profile.TopTenFragment
import me.proxer.app.fragment.profile.UserMediaListFragment
import me.proxer.app.helper.StorageHelper
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.DeviceUtils
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class ProfileActivity : ImageTabsActivity() {

    companion object {
        private const val USER_ID_EXTRA = "user_id"
        private const val USERNAME_EXTRA = "username"
        private const val IMAGE_ID_EXTRA = "image_id"

        private const val ANIME_SUB_SECTION = "anime"
        private const val MANGA_SUB_SECTION = "manga"

        fun navigateTo(context: Activity, userId: String? = null, username: String? = null, image: String? = null,
                       imageView: ImageView? = null) {
            if (userId.isNullOrBlank() && username.isNullOrBlank()) {
                return
            }

            context.intentFor<ProfileActivity>(
                    USER_ID_EXTRA to userId,
                    USERNAME_EXTRA to username,
                    IMAGE_ID_EXTRA to image
            ).let { ActivityUtils.navigateToWithImageTransition(it, context, imageView) }
        }
    }

    var userId: String?
        get() = when {
            intent.action == Intent.ACTION_VIEW -> intent.data.pathSegments.getOrNull(1)
            else -> intent.getStringExtra(USER_ID_EXTRA)
        }
        set(value) {
            intent.putExtra(USER_ID_EXTRA, value)
        }

    var username: String?
        get() = intent.getStringExtra(USERNAME_EXTRA)
        set(value) {
            intent.putExtra(USERNAME_EXTRA, value)

            title = value
        }

    var image: String?
        get() = intent.getStringExtra(IMAGE_ID_EXTRA)
        set(value) {
            intent.putExtra(IMAGE_ID_EXTRA, value)

            loadImage()
        }

    override val sectionsPagerAdapter by lazy { SectionsPagerAdapter(supportFragmentManager) }

    override val headerImageUrl
        get() = if (image.isNullOrBlank()) null else ProxerUrls.userImage(image!!)

    override val itemToDisplay: Int
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data.pathSegments.getOrNull(2)) {
                ANIME_SUB_SECTION -> 2
                MANGA_SUB_SECTION -> 3
                else -> 0
            }
            else -> 0
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val user = StorageHelper.user

        if (user == null || (user.id != userId && !user.name.equals(username, true))) {
            IconicsMenuInflatorUtil.inflate(menuInflater, this, R.menu.activity_profile, menu, true)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_chat -> {
                username?.let { safeUsername ->
                    image?.let { safeImage ->
                        doAsync {
                            val existingChat = chatDb.findConferenceForUser(safeUsername)

                            when (existingChat) {
                                null -> {
                                    weakRef.get()?.let {
                                        NewChatActivity.navigateTo(it, false, Participant(safeUsername, safeImage))
                                    }
                                }
                                else -> weakRef.get()?.let { ChatActivity.navigateTo(it, existingChat) }
                            }
                        }
                    }
                }

                return true
            }
            R.id.new_group -> {
                username?.let { safeUsername ->
                    image?.let { safeImage ->
                        NewChatActivity.navigateTo(this, true, Participant(safeUsername, safeImage))
                    }
                }

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun setupToolbar() {
        super.setupToolbar()

        title = username
    }

    override fun loadEmptyImage() {
        // If the image is not null, it means the user has none set. If it is null, it means we just have'nt loaded it
        // yet.
        if (image != null) {
            headerImage.setImageDrawable(IconicsDrawable(headerImage.context)
                    .icon(CommunityMaterial.Icon.cmd_account)
                    .sizeDp((DeviceUtils.getScreenWidth(this) * 0.75).toInt())
                    .paddingDp(32)
                    .backgroundColorRes(R.color.colorPrimaryLight)
                    .colorRes(R.color.colorPrimary))
        }
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> ProfileFragment.newInstance()
                1 -> TopTenFragment.newInstance()
                2 -> UserMediaListFragment.newInstance(Category.ANIME)
                3 -> UserMediaListFragment.newInstance(Category.MANGA)
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 4

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.section_profile_info)
                1 -> getString(R.string.section_top_ten)
                2 -> getString(R.string.section_user_media_list_anime)
                3 -> getString(R.string.section_user_media_list_manga)
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}
