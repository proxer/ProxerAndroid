package me.proxer.app.media.list

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import com.jakewharton.rxbinding2.view.actionViewEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindUntilEvent
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toCategory
import me.proxer.library.entitiy.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class MediaListFragment : PagedContentFragment<MediaListEntry>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"
        private const val SORT_CRITERIA_ARGUMENT = "sort_criteria"
        private const val TYPE_ARGUMENT = "type"
        private const val SEARCH_QUERY_ARGUMENT = "search_query"

        fun newInstance(category: Category) = MediaListFragment().apply {
            arguments = bundleOf(CATEGORY_ARGUMENT to category)
        }
    }

    override val isSwipeToRefreshEnabled = false
    override val emptyDataMessage = R.string.error_no_data_search

    override val viewModel: MediaListViewModel by lazy {
        ViewModelProviders.of(this).get(MediaListViewModel::class.java)
    }

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, VERTICAL)
    }

    override lateinit var innerAdapter: MediaAdapter

    private val category
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as Category

    private var sortCriteria: MediaSearchSortCriteria
        get() = arguments.getSerializable(SORT_CRITERIA_ARGUMENT) as? MediaSearchSortCriteria
                ?: MediaSearchSortCriteria.RATING
        set(value) {
            arguments.putSerializable(SORT_CRITERIA_ARGUMENT, value)

            viewModel.setSortCriteria(value)
        }

    private var type: MediaType
        get() = arguments.getSerializable(TYPE_ARGUMENT) as? MediaType ?: when (category) {
            Category.ANIME -> MediaType.ALL_ANIME
            Category.MANGA -> MediaType.ALL_MANGA
            else -> throw IllegalArgumentException("Unknown value for category")
        }
        set(value) {
            arguments.putSerializable(TYPE_ARGUMENT, value)

            viewModel.setType(value)
        }

    private var searchQuery: String?
        get() = arguments.getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            arguments.putString(SEARCH_QUERY_ARGUMENT, value)

            viewModel.setSearchQuery(value)
        }

    private val toolbar by lazy { activity.findViewById<Toolbar>(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = MediaAdapter(category)

        viewModel.setSortCriteria(sortCriteria, false)
        viewModel.setType(type, false)
        viewModel.setSearchQuery(searchQuery, false)

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { (view, entry) ->
                    MediaActivity.navigateTo(activity, entry.id, entry.name, entry.medium.toCategory(),
                            if (view.drawable != null) view else null)
                }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_media_list, menu, true)

        when (sortCriteria) {
            MediaSearchSortCriteria.RATING -> menu.findItem(R.id.rating).isChecked = true
            MediaSearchSortCriteria.CLICKS -> menu.findItem(R.id.clicks).isChecked = true
            MediaSearchSortCriteria.EPISODE_AMOUNT -> menu.findItem(R.id.episodeAmount).isChecked = true
            MediaSearchSortCriteria.NAME -> menu.findItem(R.id.name).isChecked = true
            else -> throw IllegalArgumentException("Unsupported sort criteria: $sortCriteria")
        }

        val filterSubMenu = menu.findItem(R.id.filter).subMenu

        when (category) {
            Category.ANIME -> filterSubMenu.setGroupVisible(R.id.filterManga, false)
            Category.MANGA -> filterSubMenu.setGroupVisible(R.id.filterAnime, false)
        }

        when (type) {
            MediaType.ALL_ANIME -> filterSubMenu.findItem(R.id.all_anime).isChecked = true
            MediaType.ANIMESERIES -> filterSubMenu.findItem(R.id.animeseries).isChecked = true
            MediaType.MOVIE -> filterSubMenu.findItem(R.id.movies).isChecked = true
            MediaType.OVA -> filterSubMenu.findItem(R.id.ova).isChecked = true
            MediaType.HENTAI -> filterSubMenu.findItem(R.id.hentai).isChecked = true
            MediaType.ALL_MANGA -> filterSubMenu.findItem(R.id.all_manga).isChecked = true
            MediaType.MANGASERIES -> filterSubMenu.findItem(R.id.mangaseries).isChecked = true
            MediaType.ONESHOT -> filterSubMenu.findItem(R.id.oneshot).isChecked = true
            MediaType.DOUJIN -> filterSubMenu.findItem(R.id.doujin).isChecked = true
            MediaType.HMANGA -> filterSubMenu.findItem(R.id.hmanga).isChecked = true
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }

        menu.findItem(R.id.search).let { searchItem ->
            val searchView = searchItem.actionView as SearchView

            searchItem.actionViewEvents()
                    .bindUntilEvent(this, Lifecycle.Event.ON_DESTROY)
                    .subscribe {
                        if (it.menuItem().isActionViewExpanded) {
                            searchQuery = null
                        }

                        TransitionManager.beginDelayedTransition(toolbar)
                    }

            searchView.queryTextChangeEvents()
                    .skipInitialValue()
                    .bindUntilEvent(this, Lifecycle.Event.ON_DESTROY)
                    .subscribe {
                        if (it.isSubmitted) {
                            searchQuery = it.queryText().toString()

                            searchView.clearFocus()
                        }
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
            R.id.rating -> sortCriteria = MediaSearchSortCriteria.RATING
            R.id.clicks -> sortCriteria = MediaSearchSortCriteria.CLICKS
            R.id.episodeAmount -> sortCriteria = MediaSearchSortCriteria.EPISODE_AMOUNT
            R.id.name -> sortCriteria = MediaSearchSortCriteria.NAME
            R.id.all_anime -> type = MediaType.ALL_ANIME
            R.id.animeseries -> type = MediaType.ANIMESERIES
            R.id.movies -> type = MediaType.MOVIE
            R.id.ova -> type = MediaType.OVA
            R.id.hentai -> type = MediaType.HENTAI
            R.id.all_manga -> type = MediaType.ALL_MANGA
            R.id.mangaseries -> type = MediaType.MANGASERIES
            R.id.oneshot -> type = MediaType.ONESHOT
            R.id.doujin -> type = MediaType.DOUJIN
            R.id.hmanga -> type = MediaType.HMANGA
        }

        item.isChecked = true

        return super.onOptionsItemSelected(item)
    }
}
