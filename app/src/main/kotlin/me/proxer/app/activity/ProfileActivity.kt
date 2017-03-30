package me.proxer.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.fragment.profile.ProfileFragment
import me.proxer.app.fragment.profile.TopTenFragment
import me.proxer.app.helper.StorageHelper
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.bindView
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class ProfileActivity : MainActivity() {

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

            val intent = context.intentFor<ProfileActivity>(
                    USER_ID_EXTRA to userId,
                    USERNAME_EXTRA to username,
                    IMAGE_ID_EXTRA to image
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !image.isNullOrBlank() && imageView != null) {
                context.startActivity(intent, ActivityOptionsCompat
                        .makeSceneTransitionAnimation(context, imageView, imageView.transitionName).toBundle())
            } else {
                context.startActivity(intent)
            }
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
            val changed = intent.getStringExtra(IMAGE_ID_EXTRA) != value

            intent.putExtra(IMAGE_ID_EXTRA, value)

            if (changed) {
                loadImage()
            }
        }

    private val itemToDisplay: Int
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data.pathSegments.getOrNull(2)) {
                ANIME_SUB_SECTION -> 2
                MANGA_SUB_SECTION -> 3
                else -> 0
            }
            else -> 0
        }

    private var sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val appbar: AppBarLayout by bindView(R.id.appbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val profileImage: ImageView by bindView(R.id.image)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_tabs)
        setSupportActionBar(toolbar)
        supportPostponeEnterTransition()

        initViews()

        if (savedInstanceState == null) {
            viewPager.currentItem = itemToDisplay
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val user = StorageHelper.user

        if (user == null || !user.name.equals(username, true)) {
            menuInflater.inflate(R.menu.activity_profile, menu)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_chat -> {
//                if (username != null && image != null) {
//                    val existingChat = chatDatabase.getChat(username!!)
//
//                    when (existingChat) {
//                        null -> NewChatActivity.navigateTo(this, Participant(username!!, image!!))
//                        else -> ChatActivity.navigateTo(this, existingChat)
//                    }
//                }
            }
            R.id.new_group -> {
//                if (username != null && image != null) {
//                    NewChatActivity.navigateTo(this, Participant(username!!, image!!),
//                            isGroup = true)
//                }
            }
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        viewPager.adapter = sectionsPagerAdapter

        title = username
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = false

        appbar.addOnOffsetChangedListener { _, verticalOffset ->
            val shadowNeeded = collapsingToolbar.height + verticalOffset > collapsingToolbar.scrimVisibleHeightTrigger

            listOf(tabs, toolbar).forEach {
                it.applyRecursively {
                    if (it is TextView) {
                        when (shadowNeeded) {
                            true -> it.setShadowLayer(3f, 0f, 0f, ContextCompat.getColor(this, android.R.color.black))
                            false -> it.setShadowLayer(0f, 0f, 0f, 0)
                        }
                    }
                }
            }
        }

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }

        profileImage.setOnClickListener {
            if (!image.isNullOrBlank()) {
                ImageDetailActivity.navigateTo(this@ProfileActivity, it as ImageView, ProxerUrls.userImage(image!!))
            }
        }

        loadImage()
    }

    private fun loadImage() {
        if (image.isNullOrBlank()) {
            profileImage.setImageDrawable(IconicsDrawable(profileImage.context)
                    .icon(CommunityMaterial.Icon.cmd_account)
                    .sizeDp((DeviceUtils.getScreenWidth(this) * 0.75).toInt())
                    .paddingDp(32)
                    .backgroundColorRes(R.color.colorPrimaryLight)
                    .colorRes(R.color.colorPrimary))
        } else {
            Glide.with(this)
                    .load(ProxerUrls.userImage(image!!).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(object : GlideDrawableImageViewTarget(profileImage) {
                        override fun onResourceReady(resource: GlideDrawable?,
                                                     animation: GlideAnimation<in GlideDrawable>?) {
                            super.onResourceReady(resource, animation)

                            supportStartPostponedEnterTransition()
                        }
                    })
        }
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> ProfileFragment.newInstance()
                1 -> TopTenFragment.newInstance()
//                2 -> UserMediaListFragment.newInstance(CategoryParameter.ANIME)
//                3 -> UserMediaListFragment.newInstance(CategoryParameter.MANGA)
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 2 // 4

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.section_profile_info)
                1 -> getString(R.string.section_top_ten)
//                2 -> getString(R.string.fragment_user_media_list_anime_title)
//                3 -> getString(R.string.fragment_user_media_list_manga_title)
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}
