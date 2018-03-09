package me.proxer.app.ucp.media

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.user.UserMediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class UcpMediaListFragment : PagedContentFragment<UserMediaListEntry>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"
        private const val FILTER_ARGUMENT = "filter"

        fun newInstance(category: Category) = UcpMediaListFragment().apply {
            arguments = bundleOf(CATEGORY_ARGUMENT to category)
        }
    }

    override val emptyDataMessage = R.string.error_no_data_user_media_list
    override val isSwipeToRefreshEnabled = false

    override val viewModel by unsafeLazy {
        UcpMediaListViewModelProvider.get(this, category, filter)
    }

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()) + 1,
            StaggeredGridLayoutManager.VERTICAL)
    }

    private val category: Category
        get() = requireArguments().getSerializable(CATEGORY_ARGUMENT) as Category

    private var filter: UserMediaListFilterType?
        get() = requireArguments().getSerializable(FILTER_ARGUMENT) as? UserMediaListFilterType
        set(value) {
            requireArguments().putSerializable(FILTER_ARGUMENT, value)

            viewModel.filter = value
        }

    override var innerAdapter by Delegates.notNull<UcpMediaAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = UcpMediaAdapter()

        innerAdapter.clickSubject
            .autoDispose(this)
            .subscribe { (view, item) ->
                MediaActivity.navigateTo(requireActivity(), item.id, item.name, item.medium.toCategory(),
                    if (view.drawable != null) view else null)
            }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, when (category) {
            Category.ANIME -> R.menu.fragment_user_media_list_anime
            Category.MANGA -> R.menu.fragment_user_media_list_manga
        }, menu, true)

        when (filter) {
            UserMediaListFilterType.WATCHING -> menu.findItem(R.id.watching).isChecked = true
            UserMediaListFilterType.WATCHED -> menu.findItem(R.id.watched).isChecked = true
            UserMediaListFilterType.WILL_WATCH -> menu.findItem(R.id.will_watch).isChecked = true
            UserMediaListFilterType.CANCELLED -> menu.findItem(R.id.cancelled).isChecked = true
            null -> menu.findItem(R.id.all).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.watching -> filter = UserMediaListFilterType.WATCHING
            R.id.watched -> filter = UserMediaListFilterType.WATCHED
            R.id.will_watch -> filter = UserMediaListFilterType.WILL_WATCH
            R.id.cancelled -> filter = UserMediaListFilterType.CANCELLED
            R.id.all -> filter = null
        }

        item.isChecked = true

        return true
    }
}
