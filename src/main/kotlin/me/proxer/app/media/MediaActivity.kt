package me.proxer.app.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.core.app.ShareCompat
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.ImageTabsActivity
import me.proxer.app.media.comments.CommentsFragment
import me.proxer.app.media.discussion.DiscussionFragment
import me.proxer.app.media.episode.EpisodeFragment
import me.proxer.app.media.info.MediaInfoFragment
import me.proxer.app.media.recommendation.RecommendationFragment
import me.proxer.app.media.relation.RelationFragment
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

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
        private const val EPISODES_ALTERNATIVE_SUB_SECTION = "list"
        private const val RELATIONS_SUB_SECTION = "relation"
        private const val RECOMMENDATIONS_SUB_SECTION = "recommendations"
        private const val DISCUSSIONS_SUB_SECTION = "forum"

        fun navigateTo(
            context: Activity,
            id: String,
            name: String? = null,
            category: Category? = null,
            imageView: ImageView? = null
        ) {
            getIntent(context, id, name, category).let {
                ActivityUtils.navigateToWithImageTransition(it, context, imageView)
            }
        }

        fun getIntent(
            context: Context,
            id: String,
            name: String? = null,
            category: Category? = null
        ): Intent {
            return context.intentFor<MediaActivity>(
                ID_EXTRA to id,
                NAME_EXTRA to name,
                CATEGORY_EXTRA to category
            )
        }
    }

    val id: String
        get() = when (intent.hasExtra(ID_EXTRA)) {
            true -> intent.getSafeStringExtra(ID_EXTRA)
            false -> intent.data?.pathSegments?.getOrNull(1) ?: "-1"
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
            intent.putExtra(CATEGORY_EXTRA, value)

            sectionsPagerAdapter.notifyDataSetChanged()
        }

    override val headerImageUrl: HttpUrl by unsafeLazy { ProxerUrls.entryImage(id) }
    override val sectionsPagerAdapter: FragmentStateAdapter by unsafeLazy { SectionsPagerAdapter() }
    override val sectionsTabCallback: TabLayoutMediator.TabConfigurationStrategy by unsafeLazy { SectionsTabCallback() }

    private val customItemToDisplay: Int
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data?.pathSegments?.getOrNull(2)) {
                COMMENTS_SUB_SECTION -> 1
                EPISODES_SUB_SECTION, EPISODES_ALTERNATIVE_SUB_SECTION -> 2
                RELATIONS_SUB_SECTION -> 3
                RECOMMENDATIONS_SUB_SECTION -> 4
                DISCUSSIONS_SUB_SECTION -> 5
                else -> 0
            }
            else -> 0
        }

    private val viewModel by viewModel<MediaInfoViewModel> { parametersOf(id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.data.observe(
            this,
            Observer { entry ->
                if (entry != null) {
                    name = entry.name
                    category = entry.category

                    if (viewPager.currentItem == 0) {
                        viewPager.currentItem = customItemToDisplay
                    }
                }
            }
        )

        preferenceHelper.isAgeRestrictedMediaAllowedObservable
            .autoDisposable(this.scope())
            .subscribe { sectionsPagerAdapter.notifyDataSetChanged() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> name?.let {
                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(getString(R.string.share_media, it, ProxerUrls.infoWeb(id)))
                    .setType("text/plain")
                    .setChooserTitle(getString(R.string.share_title))
                    .startChooser()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun setupToolbar() {
        super.setupToolbar()

        title = name
    }

    private inner class SectionsPagerAdapter : FragmentStateAdapter(supportFragmentManager, lifecycle) {

        override fun getItemCount(): Int {
            return when {
                viewModel.data.value != null || preferenceHelper.isAgeRestrictedMediaAllowed -> when (category) {
                    Category.ANIME, Category.MANGA -> 6
                    Category.NOVEL -> 5
                    null -> 1
                }
                else -> 1
            }
        }

        override fun createFragment(position: Int) = when (category) {
            Category.ANIME, Category.MANGA -> when (position) {
                0 -> MediaInfoFragment.newInstance()
                1 -> CommentsFragment.newInstance()
                2 -> EpisodeFragment.newInstance()
                3 -> RelationFragment.newInstance()
                4 -> RecommendationFragment.newInstance()
                5 -> DiscussionFragment.newInstance()
                else -> error("Unknown index passed: $position")
            }
            Category.NOVEL -> when (position) {
                0 -> MediaInfoFragment.newInstance()
                1 -> CommentsFragment.newInstance()
                2 -> RelationFragment.newInstance()
                3 -> RecommendationFragment.newInstance()
                4 -> DiscussionFragment.newInstance()
                else -> error("Unknown index passed: $position")
            }
            null -> MediaInfoFragment.newInstance()
        }

        override fun getItemId(position: Int) = when (category) {
            Category.ANIME, Category.MANGA -> position.toLong()
            Category.NOVEL -> position.toLong()
            null -> -1L
        }

        override fun containsItem(itemId: Long) = when (category) {
            Category.ANIME, Category.MANGA -> itemId in 0..5
            Category.NOVEL -> itemId in 0..4
            null -> itemId == -1L
        }
    }

    private inner class SectionsTabCallback : TabLayoutMediator.TabConfigurationStrategy {

        override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
            tab.text = when (category) {
                Category.ANIME, Category.MANGA -> when (position) {
                    0 -> getString(R.string.section_media_info)
                    1 -> getString(R.string.section_comments)
                    2 ->
                        category?.toEpisodeAppString(this@MediaActivity)
                            ?: getString(R.string.category_anime_episodes_title)
                    3 -> getString(R.string.section_relations)
                    4 -> getString(R.string.section_recommendations)
                    5 -> getString(R.string.section_discussions)
                    else -> error("Unknown index passed: $position")
                }
                else -> when (position) {
                    0 -> getString(R.string.section_media_info)
                    1 -> getString(R.string.section_comments)
                    2 -> getString(R.string.section_relations)
                    3 -> getString(R.string.section_recommendations)
                    4 -> getString(R.string.section_discussions)
                    else -> error("Unknown index passed: $position")
                }
            }
        }
    }
}
