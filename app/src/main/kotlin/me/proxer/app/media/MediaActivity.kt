package me.proxer.app.media

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.ShareCompat
import android.view.Menu
import android.widget.ImageView
import com.jakewharton.rxbinding2.support.v7.widget.itemClicks
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.R
import me.proxer.app.base.ImageTabsActivity
import me.proxer.app.media.comment.CommentFragment
import me.proxer.app.media.info.MediaInfoFragment
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class MediaActivity : ImageTabsActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"
        private const val CATEGORY_EXTRA = "category"

        private const val COMMENTS_SUB_SECTION = "comments"
        private const val EPISODES_SUB_SECTION = "episodes"
        private const val RELATIONS_SUB_SECTION = "relation"

        fun navigateTo(context: Activity, id: String, name: String? = null, category: Category? = null,
                       imageView: ImageView? = null) {
            context.intentFor<MediaActivity>(
                    ID_EXTRA to id,
                    NAME_EXTRA to name,
                    CATEGORY_EXTRA to category
            ).let { ActivityUtils.navigateToWithImageTransition(it, context, imageView) }
        }
    }

    val id: String
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data.pathSegments.getOrElse(1, { "-1" })
            else -> intent.getStringExtra(ID_EXTRA)
        }

    var name: String?
        get() = intent.getStringExtra(NAME_EXTRA)
        set(value) {
            intent.putExtra(NAME_EXTRA, value)

            title = value
        }

    var category: Category?
        get() = intent.getSerializableExtra(CATEGORY_EXTRA) as Category?
        set(value) {
            intent.putExtra(CATEGORY_EXTRA, category)

            value?.let {
                sectionsPagerAdapter.updateEpisodesTitle(value)
            }
        }

    override val headerImageUrl by lazy { ProxerUrls.entryImage(id) }
    override val sectionsPagerAdapter by lazy { SectionsPagerAdapter(supportFragmentManager) }

    override val itemToDisplay: Int
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data.pathSegments.getOrNull(2)) {
                COMMENTS_SUB_SECTION -> 1
                EPISODES_SUB_SECTION -> 2
                RELATIONS_SUB_SECTION -> 3
                else -> 0
            }
            else -> 0
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar.itemClicks()
                .bindToLifecycle(this)
                .subscribe {
                    when (it.itemId) {
                        R.id.action_share -> {
                            name?.let {
                                ShareCompat.IntentBuilder
                                        .from(this)
                                        .setText(getString(R.string.share_media, it, "https://proxer.me/info/$id"))
                                        .setType("text/plain")
                                        .setChooserTitle(getString(R.string.share_title))
                                        .startChooser()
                            }
                        }
                    }
                }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun setupToolbar() {
        super.setupToolbar()

        title = name
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int) = when (position) {
            0 -> MediaInfoFragment.newInstance()
            1 -> CommentFragment.newInstance()
//                2 -> EpisodesFragment.newInstance()
//                3 -> RelationsFragment.newInstance()
            else -> throw RuntimeException("Unknown index passed")
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): String = when (position) {
            0 -> getString(R.string.section_media_info)
            1 -> getString(R.string.section_comments)
//                2 -> category?.toEpisodeAppString(this@MediaActivity)
//                        ?: getString(R.string.category_anime_episodes_title)
//                3 -> getString(R.string.section_relations)
            else -> throw RuntimeException("Unknown index passed")
        }

        fun updateEpisodesTitle(category: Category) {
            tabs.getTabAt(2)?.text = category.toEpisodeAppString(this@MediaActivity)
        }
    }
}
