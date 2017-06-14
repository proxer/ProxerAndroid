package me.proxer.app.fragment.profile

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.mikepenz.iconics.utils.IconicsMenuInflatorUtil
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.adapter.profile.UserMediaAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toCategory
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.user.UserMediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class UserMediaListFragment : PagedLoadingFragment<ProxerCall<List<UserMediaListEntry>>, UserMediaListEntry>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"
        private const val FILTER_ARGUMENT = "filter"

        fun newInstance(category: Category): UserMediaListFragment {
            return UserMediaListFragment().apply {
                arguments = bundleOf(CATEGORY_ARGUMENT to category)
            }
        }
    }

    override val itemsOnPage = 30
    override val emptyResultMessage = R.string.error_no_data_user_media_list

    private val profileActivity
        get() = activity as ProfileActivity

    private val userId: String?
        get() = profileActivity.userId

    private val username: String?
        get() = profileActivity.username

    private val category: Category
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as Category

    private var filter: UserMediaListFilterType?
        get() = arguments.getSerializable(FILTER_ARGUMENT) as? UserMediaListFilterType
        set(value) = arguments.putSerializable(FILTER_ARGUMENT, value)

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, StaggeredGridLayoutManager.VERTICAL)
    }

    override val innerAdapter by lazy { UserMediaAdapter(GlideApp.with(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.callback = object : UserMediaAdapter.UserMediaAdapterCallback {
            override fun onMediaClick(view: View, item: UserMediaListEntry) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, item.id, item.name, item.medium.toCategory(),
                        if (imageView.drawable != null) imageView else null)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflatorUtil.inflate(inflater, context, when (category) {
            Category.ANIME -> R.menu.fragment_user_media_list_anime
            Category.MANGA -> R.menu.fragment_user_media_list_manga
        }, menu, true)

        when (filter) {
            UserMediaListFilterType.WATCHING -> menu.findItem(R.id.watching).isChecked = true
            UserMediaListFilterType.WATCHED -> menu.findItem(R.id.watched).isChecked = true
            UserMediaListFilterType.WILL_WATCH -> menu.findItem(R.id.will_watch).isChecked = true
            UserMediaListFilterType.CANCELLED -> menu.findItem(R.id.cancelled).isChecked = true
            null -> {
                // Nothing to do.
            }
            else -> throw IllegalArgumentException("Unsupported filter: $filter")
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousFilter = filter

        when (item.itemId) {
            R.id.watching -> filter = UserMediaListFilterType.WATCHING
            R.id.watched -> filter = UserMediaListFilterType.WATCHED
            R.id.will_watch -> filter = UserMediaListFilterType.WILL_WATCH
            R.id.cancelled -> filter = UserMediaListFilterType.CANCELLED
            else -> return false
        }

        if (previousFilter != filter) {
            item.isChecked = true

            freshLoad()
        }

        return true
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<UserMediaListEntry>>().build()
    override fun constructPagedInput(page: Int) = api.user().mediaList(userId, username)
            .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(context) && StorageHelper.user != null)
            .page(page)
            .limit(itemsOnPage)
            .category(category)
            .filter(filter)
            .build()
}
