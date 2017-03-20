package me.proxer.app.fragment.media

import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.proxerme.library.api.ProxerCall
import com.proxerme.library.entitiy.list.MediaListEntry
import com.proxerme.library.enums.Category
import com.proxerme.library.enums.MediaSearchSortCriteria
import com.proxerme.library.enums.MediaType
import me.proxer.app.R
import me.proxer.app.adapter.media.MediaAdapter
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.ProxerTask
import me.proxer.app.util.extension.api

/**
 * @author Ruben Gees
 */
class MediaListFragment : PagedLoadingFragment<ProxerCall<List<MediaListEntry>>, MediaListEntry>() {

    companion object {
        private const val ARGUMENT_CATEGORY = "category"
        private const val SEARCH_QUERY_STATE = "search_query"
        private const val HAS_SEARCHED_STATE = "has_searched"

        fun newInstance(category: Category): MediaListFragment {
            return MediaListFragment().apply {
                this.arguments = Bundle().apply {
                    this.putSerializable(ARGUMENT_CATEGORY, category)
                }
            }
        }
    }

    override val isLoginRequired: Boolean
        get() = type == MediaType.HENTAI || type == MediaType.HMANGA
    override val isHentaiConfirmationRequired: Boolean
        get() = type == MediaType.HENTAI || type == MediaType.HMANGA

    override val spanCount: Int
        get() = super.spanCount + 1

    private val category: Category
        get() = arguments.getSerializable(ARGUMENT_CATEGORY) as Category

    private var sortCriteria = MediaSearchSortCriteria.RATING
    private lateinit var type: MediaType

    private var searchQuery: String? = null
    private var hasSearched = false

    override lateinit var innerAdapter: MediaAdapter
    override val itemsOnPage = 30

    private lateinit var searchItem: MenuItem
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = when (category) {
            Category.ANIME -> MediaType.ALL_ANIME
            Category.MANGA -> MediaType.ALL_MANGA
            else -> throw IllegalArgumentException("Unknown value for category")
        }

        innerAdapter = MediaAdapter(category)
        innerAdapter.callback = object : MediaAdapter.MediaAdapterCallback {
            override fun onMediaClick(item: MediaListEntry) {
//                MediaActivity.navigateTo(activity, item.id, item.name,
//                        ParameterMapper.mediumToCategory(item.medium) ?: CategoryParameter.ANIME)
            }
        }

        searchQuery = savedInstanceState?.getString(SEARCH_QUERY_STATE)
        hasSearched = savedInstanceState?.getBoolean(HAS_SEARCHED_STATE) ?: false

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_media_list, menu)

        when (sortCriteria) {
            MediaSearchSortCriteria.RATING -> menu.findItem(R.id.rating).isChecked = true
            MediaSearchSortCriteria.CLICKS -> menu.findItem(R.id.clicks).isChecked = true
            MediaSearchSortCriteria.EPISODE_AMOUNT -> menu.findItem(R.id.episodeAmount).isChecked = true
            MediaSearchSortCriteria.NAME -> menu.findItem(R.id.name).isChecked = true
            else -> throw IllegalArgumentException("Unsupported sort criteria: ${sortCriteria}")
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
            MediaType.ONESHOT -> filterSubMenu.findItem(R.id.oneshot).isChecked = true
            MediaType.DOUJIN -> filterSubMenu.findItem(R.id.doujin).isChecked = true
            MediaType.HMANGA -> filterSubMenu.findItem(R.id.hmanga).isChecked = true
            else -> throw IllegalArgumentException("Unsupported type: ${type}")
        }

        searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView

        if (searchQuery != null) {
            searchItem.expandActionView()
            searchView.setQuery(searchQuery, false)
            searchView.clearFocus()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hasSearched = true

                searchView.clearFocus()
                freshLoad()

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText

                return false
            }
        })

        MenuItemCompat.setOnActionExpandListener(searchItem,
                object : MenuItemCompat.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                        searchQuery = null

                        if (hasSearched) {
                            hasSearched = false

                            freshLoad()
                        }

                        return true
                    }
                })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCriteria = sortCriteria
        val previousType = type

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
            R.id.oneshot -> type = MediaType.ONESHOT
            R.id.doujin -> type = MediaType.DOUJIN
            R.id.hmanga -> type = MediaType.HMANGA
            else -> return false
        }

        if (previousCriteria != sortCriteria || previousType != type) {
            item.isChecked = true

            freshLoad()
        }

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(SEARCH_QUERY_STATE, searchQuery)
        outState.putBoolean(HAS_SEARCHED_STATE, hasSearched)
    }

    override fun onDestroyView() {
        searchView.setOnQueryTextListener(null)
        MenuItemCompat.setOnActionExpandListener(searchItem, null)

        super.onDestroyView()
    }

    override fun constructTask() = ProxerTask<List<MediaListEntry>>()
    override fun constructPagedInput(page: Int): ProxerCall<List<MediaListEntry>> {
        return api.list().mediaSearch()
                .page(page)
                .limit(itemsOnPage)
                .name(searchQuery)
                .type(type)
                .sort(sortCriteria)
                .build()
    }
}
