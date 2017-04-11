package me.proxer.app.fragment.media

import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.media.MediaAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.extension.toCategory
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class MediaListFragment : PagedLoadingFragment<ProxerCall<List<MediaListEntry>>, MediaListEntry>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"
        private const val SORT_CRITERIA_ARGUMENT = "sort_criteria"
        private const val TYPE_ARGUMENT = "type"
        private const val SEARCH_QUERY_ARGUMENT = "search_query"
        private const val HAS_SEARCHED_ARGUMENT = "has_searched"

        fun newInstance(category: Category): MediaListFragment {
            return MediaListFragment().apply {
                arguments = bundleOf(CATEGORY_ARGUMENT to category)
            }
        }
    }

    override val isLoginRequired: Boolean
        get() = type == MediaType.HENTAI || type == MediaType.HMANGA
    override val isAgeConfirmationRequired: Boolean
        get() = type == MediaType.HENTAI || type == MediaType.HMANGA

    override val spanCount get() = super.spanCount + 1
    override val emptyResultMessage = R.string.error_no_data_media_list

    private val category
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as Category

    private var sortCriteria: MediaSearchSortCriteria
        get() = arguments.getSerializable(SORT_CRITERIA_ARGUMENT) as? MediaSearchSortCriteria
                ?: MediaSearchSortCriteria.RATING
        set(value) = arguments.putSerializable(SORT_CRITERIA_ARGUMENT, value)

    private var type: MediaType
        get() = arguments.getSerializable(TYPE_ARGUMENT) as? MediaType ?: when (category) {
            Category.ANIME -> MediaType.ALL_ANIME
            Category.MANGA -> MediaType.ALL_MANGA
            else -> throw IllegalArgumentException("Unknown value for category")
        }
        set(value) = arguments.putSerializable(TYPE_ARGUMENT, value)

    private var searchQuery: String?
        get() = arguments.getString(SEARCH_QUERY_ARGUMENT)
        set(value) = arguments.putString(SEARCH_QUERY_ARGUMENT, value)

    private var hasSearched: Boolean
        get() = arguments.getBoolean(HAS_SEARCHED_ARGUMENT, false)
        set(value) = arguments.putBoolean(HAS_SEARCHED_ARGUMENT, value)

    override val itemsOnPage = 30
    override val innerAdapter by lazy { MediaAdapter(category) }

    private lateinit var searchItem: MenuItem
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.callback = object : MediaAdapter.MediaAdapterCallback {
            override fun onMediaClick(view: View, item: MediaListEntry) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, item.id, item.name, item.medium.toCategory(),
                        if (imageView.drawable != null) imageView else null)
            }
        }

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
            MediaType.MANGASERIES -> filterSubMenu.findItem(R.id.mangaseries).isChecked = true
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
                        TransitionManager.beginDelayedTransition(activity.find(R.id.toolbar))

                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                        searchQuery = null

                        if (hasSearched) {
                            hasSearched = false

                            freshLoad()
                        }

                        TransitionManager.beginDelayedTransition(activity.find(R.id.toolbar))

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
            R.id.mangaseries -> type = MediaType.MANGASERIES
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

    override fun onDestroyView() {
        searchView.setOnQueryTextListener(null)
        MenuItemCompat.setOnActionExpandListener(searchItem, null)

        super.onDestroyView()
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<MediaListEntry>>().build()
    override fun constructPagedInput(page: Int) = api.list().mediaSearch()
            .page(page)
            .limit(itemsOnPage)
            .name(searchQuery)
            .type(type)
            .sort(sortCriteria)
            .build()
}
