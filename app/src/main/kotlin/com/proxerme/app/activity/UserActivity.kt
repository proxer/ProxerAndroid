package com.proxerme.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.activity.chat.ChatActivity
import com.proxerme.app.activity.chat.NewChatActivity
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.Participant
import com.proxerme.app.fragment.user.ProfileFragment
import com.proxerme.app.fragment.user.ToptenFragment
import com.proxerme.app.fragment.user.UserMediaListFragment
import com.proxerme.app.manager.UserManager
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.user.entitiy.UserInfo
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import org.jetbrains.anko.intentFor

class UserActivity : MainActivity() {

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"
        private const val EXTRA_USERNAME = "extra_username"
        private const val EXTRA_IMAGE_ID = "extra_image_id"
        private const val STATE_IMAGE_ID = "activity_user_image_id"

        fun navigateTo(context: Activity, userId: String? = null, username: String? = null,
                       imageId: String? = null) {
            if (userId.isNullOrBlank() && username.isNullOrBlank()) {
                return
            }

            context.startActivity(context.intentFor<UserActivity>(
                    EXTRA_USER_ID to userId,
                    EXTRA_USERNAME to username,
                    EXTRA_IMAGE_ID to imageId
            ))
        }
    }

    private var userId: String? = null
    private var username: String? = null
    private var imageId: String? = null
    private var sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val profileImage: ImageView by bindView(R.id.profileImage)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user)
        setSupportActionBar(toolbar)

        userId = if (intent.action == Intent.ACTION_VIEW) {
            intent.data.pathSegments.getOrElse(1, { "-1" })
        } else {
            intent.getStringExtra(EXTRA_USER_ID)
        }

        username = intent.getStringExtra(EXTRA_USERNAME)

        if (savedInstanceState == null) {
            imageId = intent.getStringExtra(EXTRA_IMAGE_ID)
        } else {
            imageId = savedInstanceState.getString(STATE_IMAGE_ID)
        }

        initViews()

        if (savedInstanceState == null) {
            viewPager.currentItem = if (intent.action == Intent.ACTION_VIEW) {
                when (intent.data.pathSegments.getOrNull(2)) {
                    "anime" -> 2
                    "manga" -> 3
                    else -> 0
                }
            } else {
                0
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (UserManager.user == null || UserManager.user!!.username != username) {
            menuInflater.inflate(R.menu.activity_user, menu)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_chat -> {
                if (username != null && imageId != null) {
                    val existingChat = chatDatabase.getChat(username!!)

                    if (existingChat == null) {
                        NewChatActivity.navigateTo(this, Participant(username!!, imageId!!))
                    } else {
                        ChatActivity.navigateTo(this, existingChat)
                    }
                }
            }
            R.id.new_group -> {
                if (username != null && imageId != null) {
                    NewChatActivity.navigateTo(this, Participant(username!!, imageId!!),
                            isGroup = true)
                }
            }
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_IMAGE_ID, imageId)
    }

    fun setUserInfo(userInfo: UserInfo) {
        if (this.userId == null) {
            this.userId = userInfo.id
        }

        if (this.username == null) {
            this.username = userInfo.username

            supportActionBar?.title = username
        }

        if (this.imageId == null) {
            this.imageId = userInfo.imageId

            loadImage()
        }
    }

    private fun initViews() {
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = sectionsPagerAdapter

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = username
        collapsingToolbar.isTitleEnabled = false
        tabs.setupWithViewPager(viewPager)

        profileImage.setOnClickListener {
            if (!imageId.isNullOrBlank()) {
                ImageDetailActivity.navigateTo(this@UserActivity, it as ImageView,
                        ProxerUrlHolder.getUserImageUrl(imageId!!))
            }
        }

        loadImage()
    }

    private fun loadImage() {
        if (imageId.isNullOrBlank()) {
            profileImage.setImageDrawable(IconicsDrawable(profileImage.context)
                    .icon(CommunityMaterial.Icon.cmd_account)
                    .sizeDp(256)
                    .paddingDp(32)
                    .backgroundColorRes(R.color.colorPrimaryLight)
                    .colorRes(R.color.colorPrimary))
        } else {
            Glide.with(this)
                    .load(ProxerUrlHolder.getUserImageUrl(imageId!!).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(profileImage)
        }
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) :
            FragmentStatePagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> ProfileFragment.newInstance(userId, username)
                1 -> ToptenFragment.newInstance(userId, username)
                2 -> UserMediaListFragment.newInstance(userId, username, CategoryParameter.ANIME)
                3 -> UserMediaListFragment.newInstance(userId, username, CategoryParameter.MANGA)
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 4

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.fragment_profile_title)
                1 -> getString(R.string.fragment_topten_title)
                2 -> getString(R.string.fragment_user_media_list_anime_title)
                3 -> getString(R.string.fragment_user_media_list_manga_title)
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}
