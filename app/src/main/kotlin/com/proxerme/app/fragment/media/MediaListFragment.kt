package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.media.MediaAdapter
import com.proxerme.app.adapter.media.MediaAdapter.MediaAdapterCallback
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.fragment.media.MediaListFragment.MediaInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.list.entity.MediaListEntry
import com.proxerme.library.connection.list.request.MediaSearchRequest
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.MediaSortParameter
import com.proxerme.library.parameters.TypeParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaListFragment : PagedLoadingFragment<MediaInput, MediaListEntry>() {

    companion object {

        private const val ARGUMENT_CATEGORY = "category"

        fun newInstance(@CategoryParameter.Category category: String): MediaListFragment {
            return MediaListFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_CATEGORY, category)
                }
            }
        }
    }

    override val section = Section.MEDIA_LIST
    override val itemsOnPage = 30
    override val isSwipeToRefreshEnabled = false
    override val isHentaiConfirmationRequired: Boolean
        get() = type == TypeParameter.HENTAI || type == TypeParameter.HMANGA

    @CategoryParameter.Category
    private lateinit var category: String

    @MediaSortParameter.MediaSortCriteria
    private lateinit var sortCriteria: String

    @TypeParameter.Type
    private lateinit var type: String

    private var searchQuery: String? = null
    private var hasSearched = false

    override lateinit var adapter: MediaAdapter
    override lateinit var layoutManager: StaggeredGridLayoutManager

    private lateinit var searchItem: MenuItem
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        category = arguments.getString(ARGUMENT_CATEGORY)

        sortCriteria = MediaSortParameter.RATING
        type = when (category) {
            CategoryParameter.ANIME -> TypeParameter.ALL_ANIME
            CategoryParameter.MANGA -> TypeParameter.ALL_MANGA
            else -> throw IllegalArgumentException("Unknown value for category")
        }

        adapter = MediaAdapter(category)
        adapter.callback = object : MediaAdapterCallback() {
            override fun onItemClick(item: MediaListEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_media_list, menu)

        when (sortCriteria) {
            MediaSortParameter.RATING -> menu.findItem(R.id.rating).isChecked = true
            MediaSortParameter.CLICKS -> menu.findItem(R.id.clicks).isChecked = true
            MediaSortParameter.COUNT -> menu.findItem(R.id.count).isChecked = true
            MediaSortParameter.NAME -> menu.findItem(R.id.name).isChecked = true
        }

        val filterSubMenu = menu.findItem(R.id.filter).subMenu

        when (category) {
            CategoryParameter.ANIME -> filterSubMenu.setGroupVisible(R.id.filterManga, false)
            CategoryParameter.MANGA -> filterSubMenu.setGroupVisible(R.id.filterAnime, false)
        }

        when (type) {
            TypeParameter.ALL_ANIME -> filterSubMenu.findItem(R.id.all_anime).isChecked = true
            TypeParameter.ANIMESERIES -> filterSubMenu.findItem(R.id.animeseries).isChecked = true
            TypeParameter.MOVIE -> filterSubMenu.findItem(R.id.movies).isChecked = true
            TypeParameter.OVA -> filterSubMenu.findItem(R.id.ova).isChecked = true
            TypeParameter.HENTAI -> filterSubMenu.findItem(R.id.hentai).isChecked = true
            TypeParameter.ALL_MANGA -> filterSubMenu.findItem(R.id.all_manga).isChecked = true
            TypeParameter.ONESHOT -> filterSubMenu.findItem(R.id.oneshot).isChecked = true
            TypeParameter.DOUJIN -> filterSubMenu.findItem(R.id.doujin).isChecked = true
            TypeParameter.HMANGA -> filterSubMenu.findItem(R.id.hmanga).isChecked = true
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
                reset()

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
                            reset()

                            hasSearched = false
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
            R.id.rating -> sortCriteria = MediaSortParameter.RATING
            R.id.clicks -> sortCriteria = MediaSortParameter.CLICKS
            R.id.count -> sortCriteria = MediaSortParameter.COUNT
            R.id.name -> sortCriteria = MediaSortParameter.NAME
            R.id.all_anime -> type = TypeParameter.ALL_ANIME
            R.id.animeseries -> type = TypeParameter.ANIMESERIES
            R.id.movies -> type = TypeParameter.MOVIE
            R.id.ova -> type = TypeParameter.OVA
            R.id.hentai -> type = TypeParameter.HENTAI
            R.id.all_manga -> type = TypeParameter.ALL_MANGA
            R.id.oneshot -> type = TypeParameter.ONESHOT
            R.id.doujin -> type = TypeParameter.DOUJIN
            R.id.hmanga -> type = TypeParameter.HMANGA
            else -> return false
        }

        if (previousCriteria != sortCriteria || previousType != type) {
            reset()

            item.isChecked = true
        }

        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyOptionsMenu() {
        searchView.setOnQueryTextListener(null)
        MenuItemCompat.setOnActionExpandListener(searchItem, null)

        super.onDestroyOptionsMenu()
    }

    override fun constructTask(): ListenableTask<MediaInput, Array<MediaListEntry>> {
        return ProxerLoadingTask({
            MediaSearchRequest(it.page)
                    .withName(it.searchQuery)
                    .withType(it.type)
                    .withSortCriteria(it.sortCriteria)
                    .withLimit(it.itemsOnPage)
        })
    }

    override fun constructInput(page: Int): MediaInput {
        return MediaInput(page, searchQuery, type, sortCriteria, itemsOnPage)
    }

    class MediaInput(page: Int, val searchQuery: String?, val type: String,
                     val sortCriteria: String, val itemsOnPage: Int) : PagedInput(page)
}