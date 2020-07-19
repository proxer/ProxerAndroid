package me.proxer.app.bookmark

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.appcompat.queryTextChangeEvents
import com.jakewharton.rxbinding3.view.actionViewEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.AnimeActivity
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.Category
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class BookmarkFragment : PagedContentFragment<Bookmark>() {

    companion object {
        private const val SEARCH_QUERY_ARGUMENT = "search_query"
        private const val CATEGORY_ARGUMENT = "category"
        private const val FILTER_AVAILABLE_ARGUMENT = "filter_available"

        fun newInstance() = BookmarkFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage
        get() = when (searchQuery.isNullOrBlank()) {
            true -> R.string.error_no_data_bookmark
            false -> R.string.error_no_data_search
        }

    override val viewModel by viewModel<BookmarkViewModel> { parametersOf(searchQuery, category, filterAvailable) }

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()) + 1, VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<BookmarkAdapter>()

    private var searchQuery: String?
        get() = requireArguments().getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            requireArguments().putString(SEARCH_QUERY_ARGUMENT, value)

            viewModel.searchQuery = value
        }

    private var category: Category?
        get() = requireArguments().getSerializable(CATEGORY_ARGUMENT) as? Category
        set(value) {
            requireArguments().putSerializable(CATEGORY_ARGUMENT, value)

            viewModel.category = value
        }

    private var filterAvailable: Boolean
        get() = requireArguments().getBoolean(FILTER_AVAILABLE_ARGUMENT, false)
        set(value) {
            requireArguments().putBoolean(FILTER_AVAILABLE_ARGUMENT, value)

            viewModel.filterAvailable = value
        }

    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }

    private var searchView by Delegates.notNull<SearchView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = BookmarkAdapter()

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe {
                when (it.category) {
                    Category.ANIME -> AnimeActivity.navigateTo(
                        requireActivity(), it.entryId, it.episode,
                        it.language.toAnimeLanguage(), it.name
                    )
                    Category.MANGA, Category.NOVEL -> MangaActivity.navigateTo(
                        requireActivity(), it.entryId, it.episode,
                        it.language.toGeneralLanguage(), it.chapterName, it.name
                    )
                }
            }

        innerAdapter.longClickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, bookmark) ->
                MediaActivity.navigateTo(requireActivity(), bookmark.entryId, bookmark.name, bookmark.category, view)
            }

        innerAdapter.deleteClickSubject
            .autoDisposable(this.scope())
            .subscribe {
                viewModel.addItemToDelete(it)
            }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        viewModel.itemDeletionError.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    hostingActivity.multilineSnackbar(
                        getString(R.string.error_bookmark_deletion, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                    )
                }
            }
        )

        viewModel.undoData.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    hostingActivity.multilineSnackbar(
                        R.string.fragment_bookmark_delete_message,
                        Snackbar.LENGTH_LONG, R.string.action_undo,
                        View.OnClickListener { viewModel.undo() }
                    )
                }
            }
        )

        viewModel.undoError.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    hostingActivity.multilineSnackbar(
                        getString(R.string.error_undo, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                    )
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_bookmarks, menu, true)

        when (category) {
            Category.ANIME -> menu.findItem(R.id.anime).isChecked = true
            Category.MANGA -> menu.findItem(R.id.manga).isChecked = true
            else -> menu.findItem(R.id.all).isChecked = true
        }

        menu.findItem(R.id.filterAvailable).isChecked = filterAvailable == true

        menu.findItem(R.id.search).let { searchItem ->
            searchView = searchItem.actionView as SearchView

            searchItem.actionViewEvents()
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe { event ->
                    if (event.menuItem.isActionViewExpanded) {
                        searchQuery = null
                    }

                    TransitionManager.endTransitions(toolbar)
                    TransitionManager.beginDelayedTransition(toolbar)
                }

            searchView.queryTextChangeEvents()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe { event ->
                    if (event.isSubmitted) {
                        searchView.clearFocus()
                    }

                    searchQuery = event.queryText.toString().trim()
                }

            searchQuery?.let {
                searchItem.expandActionView()
                searchView.setQuery(it, false)
                searchView.clearFocus()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.anime -> {
                category = Category.ANIME

                item.isChecked = true
            }
            R.id.manga -> {
                category = Category.MANGA

                item.isChecked = true
            }
            R.id.all -> {
                category = null

                item.isChecked = true
            }
            R.id.filterAvailable -> {
                filterAvailable = !item.isChecked

                item.isChecked = !item.isChecked
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        innerAdapter.swapDataAndNotifyWithDiffing(emptyList())

        super.showError(action)
    }
}
