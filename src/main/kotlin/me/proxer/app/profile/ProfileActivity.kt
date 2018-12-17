package me.proxer.app.profile

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.R
import me.proxer.app.base.ImageTabsActivity
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.profile.about.ProfileAboutFragment
import me.proxer.app.profile.comment.ProfileCommentFragment
import me.proxer.app.profile.history.HistoryFragment
import me.proxer.app.profile.info.ProfileInfoFragment
import me.proxer.app.profile.media.ProfileMediaListFragment
import me.proxer.app.profile.topten.TopTenFragment
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.backgroundColorAttr
import me.proxer.app.util.extension.colorAttr
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.koin.android.ext.android.inject

/**
 * @author Ruben Gees
 */
class ProfileActivity : ImageTabsActivity() {

    companion object {
        private const val USER_ID_EXTRA = "user_id"
        private const val USERNAME_EXTRA = "username"
        private const val IMAGE_ID_EXTRA = "image_id"

        private const val ABOUT_SUB_SECTION = "about"
        private const val ANIME_SUB_SECTION = "anime"
        private const val MANGA_SUB_SECTION = "manga"
        private const val HISTORY_SUB_SECTION = "chronik"

        fun navigateTo(
            context: Activity,
            userId: String? = null,
            username: String? = null,
            image: String? = null,
            imageView: ImageView? = null
        ) {
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
            intent.action == Intent.ACTION_VIEW -> intent.data?.pathSegments?.getOrNull(1)
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

    private val messengerDao by inject<MessengerDao>()

    override val sectionsPagerAdapter by unsafeLazy { SectionsPagerAdapter(supportFragmentManager) }

    override val headerImageUrl
        get() = image.let { image ->
            if (image == null || image.isBlank()) null else ProxerUrls.userImage(image)
        }

    override val itemToDisplay: Int
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data?.pathSegments?.getOrNull(2)) {
                ABOUT_SUB_SECTION -> 1
                ANIME_SUB_SECTION -> 3
                MANGA_SUB_SECTION -> 4
                HISTORY_SUB_SECTION -> 5
                else -> 0
            }
            else -> 0
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        storageHelper.user.let {
            if (it == null || it.id != userId && !it.name.equals(username, true)) {
                IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_profile, menu, true)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create_chat -> username?.let { safeUsername ->
                image?.let { safeImage ->
                    Completable
                        .fromAction {
                            messengerDao.findConferenceForUser(safeUsername).let { existingChat ->
                                when (existingChat) {
                                    null -> CreateConferenceActivity.navigateTo(
                                        this, false, Participant(safeUsername, safeImage)
                                    )
                                    else -> MessengerActivity.navigateTo(this, existingChat)
                                }
                            }
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribeAndLogErrors()
                }
            }
            R.id.new_group -> username?.let { safeUsername ->
                image?.let { safeImage ->
                    CreateConferenceActivity.navigateTo(this, true, Participant(safeUsername, safeImage))
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun setupToolbar() {
        super.setupToolbar()

        title = username
    }

    override fun loadEmptyImage() {
        // If the image is not null, it means the user has none set.
        // If it is null, it means we just have'nt loaded it yet.
        if (image != null) {
            headerImage.setImageDrawable(
                IconicsDrawable(headerImage.context)
                    .icon(CommunityMaterial.Icon.cmd_account)
                    .sizeDp((DeviceUtils.getScreenWidth(this) * 0.75).toInt())
                    .paddingDp(32)
                    .backgroundColorAttr(headerImage.context, R.attr.colorPrimaryLight)
                    .colorAttr(headerImage.context, R.attr.colorPrimary)
            )
        }
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int) = when (position) {
            0 -> ProfileInfoFragment.newInstance()
            1 -> ProfileAboutFragment.newInstance()
            2 -> TopTenFragment.newInstance()
            3 -> ProfileMediaListFragment.newInstance(Category.ANIME)
            4 -> ProfileMediaListFragment.newInstance(Category.MANGA)
            5 -> ProfileCommentFragment.newInstance()
            6 -> HistoryFragment.newInstance()
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }

        override fun getCount() = 7

        override fun getPageTitle(position: Int): String = when (position) {
            0 -> getString(R.string.section_profile_info)
            1 -> getString(R.string.section_profile_about)
            2 -> getString(R.string.section_top_ten)
            3 -> getString(R.string.section_user_media_list_anime)
            4 -> getString(R.string.section_user_media_list_manga)
            5 -> getString(R.string.section_user_comments)
            6 -> getString(R.string.section_user_history)
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }
    }
}
