package me.proxer.app.media.list

import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import com.jakewharton.rxbinding2.view.actionViewEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BackPressAware
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.LocalTag
import me.proxer.app.media.MediaActivity
import me.proxer.app.ui.view.ExpandableSelectionView
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.getEnumSet
import me.proxer.app.util.extension.putEnumSet
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import me.proxer.library.enums.TagRateFilter
import me.proxer.library.enums.TagSpoilerFilter
import org.jetbrains.anko.bundleOf
import java.util.EnumSet
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MediaListFragment : PagedContentFragment<MediaListEntry>(), BackPressAware {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"
        private const val SORT_CRITERIA_ARGUMENT = "sort_criteria"
        private const val TYPE_ARGUMENT = "type"
        private const val SEARCH_QUERY_ARGUMENT = "search_query"
        private const val LANGUAGE_ARGUMENT = "language"
        private const val GENRES_ARGUMENT = "genres"
        private const val EXCLUDED_GENRES_ARGUMENT = "excluded_genres"
        private const val FSK_CONSTRAINTS_ARGUMENT = "fsk_constraints"
        private const val TAGS_ARGUMENT = "tags"
        private const val EXCLUDED_TAGS_ARGUMENT = "excluded_tags"
        private const val TAG_RATE_FILTER_ARGUMENT = "tag_rate_filter"
        private const val TAG_SPOILER_FILTER_ARGUMENT = "tag_spoiler_filter"

        fun newInstance(category: Category) = MediaListFragment().apply {
            arguments = bundleOf(CATEGORY_ARGUMENT to category)
        }
    }

    override val isSwipeToRefreshEnabled = false
    override val emptyDataMessage = R.string.error_no_data_search

    override val viewModel by unsafeLazy {
        MediaListViewModelProvider.get(this, sortCriteria, type, searchQuery, language,
            genres, excludedGenres, fskConstraints, tags, excludedTags, tagRateFilter, tagSpoilerFilter)
    }

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()) + 1, VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<MediaAdapter>()

    private val category
        get() = requireArguments().getSerializable(CATEGORY_ARGUMENT) as Category

    private var sortCriteria: MediaSearchSortCriteria
        get() = requireArguments().getSerializable(SORT_CRITERIA_ARGUMENT) as? MediaSearchSortCriteria
            ?: MediaSearchSortCriteria.RATING
        set(value) {
            requireArguments().putSerializable(SORT_CRITERIA_ARGUMENT, value)

            viewModel.sortCriteria = value
        }

    private var type: MediaType
        get() = requireArguments().getSerializable(TYPE_ARGUMENT) as? MediaType ?: when (category) {
            Category.ANIME -> MediaType.ALL_ANIME
            Category.MANGA -> MediaType.ALL_MANGA
            else -> throw IllegalArgumentException("Unknown value for category")
        }
        set(value) {
            requireArguments().putSerializable(TYPE_ARGUMENT, value)

            viewModel.type = value
        }

    private var searchQuery: String?
        get() = requireArguments().getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            requireArguments().putString(SEARCH_QUERY_ARGUMENT, value)

            viewModel.searchQuery = value
        }

    internal var language: Language?
        get() = requireArguments().getSerializable(LANGUAGE_ARGUMENT) as? Language?
        set(value) {
            requireArguments().putSerializable(LANGUAGE_ARGUMENT, language)

            viewModel.language = value
        }

    internal var genres: List<LocalTag>
        get() = requireArguments().getParcelableArrayList(GENRES_ARGUMENT) ?: emptyList()
        set(value) {
            requireArguments().putParcelableArrayList(GENRES_ARGUMENT, ArrayList(value))

            viewModel.genres = value
        }

    internal var excludedGenres: List<LocalTag>
        get() = requireArguments().getParcelableArrayList(EXCLUDED_GENRES_ARGUMENT) ?: emptyList()
        set(value) {
            requireArguments().putParcelableArrayList(EXCLUDED_GENRES_ARGUMENT, ArrayList(value))

            viewModel.excludedGenres = value
        }

    internal var fskConstraints: EnumSet<FskConstraint>
        get() = requireArguments().getEnumSet(FSK_CONSTRAINTS_ARGUMENT, FskConstraint::class.java)
        set(value) {
            requireArguments().putEnumSet(FSK_CONSTRAINTS_ARGUMENT, value)

            viewModel.fskConstraints = value
        }

    internal var tags: List<LocalTag>
        get() = requireArguments().getParcelableArrayList(TAGS_ARGUMENT) ?: emptyList()
        set(value) {
            requireArguments().putParcelableArrayList(TAGS_ARGUMENT, ArrayList(value))

            viewModel.tags = value
        }

    internal var excludedTags: List<LocalTag>
        get() = requireArguments().getParcelableArrayList(EXCLUDED_TAGS_ARGUMENT) ?: emptyList()
        set(value) {
            requireArguments().putParcelableArrayList(EXCLUDED_TAGS_ARGUMENT, ArrayList(value))

            viewModel.excludedTags = value
        }

    internal var tagRateFilter: TagRateFilter
        get() = requireArguments().getSerializable(TAG_RATE_FILTER_ARGUMENT) as? TagRateFilter
            ?: TagRateFilter.RATED_ONLY
        set(value) {
            requireArguments().putSerializable(TAG_RATE_FILTER_ARGUMENT, value)

            viewModel.tagRateFilter = value
        }

    internal var tagSpoilerFilter: TagSpoilerFilter
        get() = requireArguments().getSerializable(TAG_SPOILER_FILTER_ARGUMENT) as? TagSpoilerFilter
            ?: TagSpoilerFilter.NO_SPOILERS
        set(value) {
            requireArguments().putSerializable(TAG_SPOILER_FILTER_ARGUMENT, value)

            viewModel.tagSpoilerFilter = value
        }

    private var searchBottomSheetManager by Delegates.notNull<MediaListSearchBottomSheet>()

    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }

    internal val searchBottomSheet by bindView<ViewGroup>(R.id.searchBottomSheet)
    internal val searchBottomSheetTitle by bindView<ViewGroup>(R.id.titleContainer)
    internal val search by bindView<Button>(R.id.search)
    internal val languageSelector by bindView<ExpandableSelectionView>(R.id.languageSelector)
    internal val genreSelector by bindView<ExpandableSelectionView>(R.id.genreSelector)
    internal val excludedGenreSelector by bindView<ExpandableSelectionView>(R.id.excludedGenreSelector)
    internal val fskSelector by bindView<ExpandableSelectionView>(R.id.fskSelector)
    internal val tagSelector by bindView<ExpandableSelectionView>(R.id.tagSelector)
    internal val excludedTagSelector by bindView<ExpandableSelectionView>(R.id.excludedTagSelector)
    internal val includeUnratedTags by bindView<CheckBox>(R.id.includeUnratedTags)
    internal val includeSpoilerTags by bindView<CheckBox>(R.id.includeSpoilerTags)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = MediaAdapter(category)

        innerAdapter.clickSubject
            .autoDispose(this)
            .subscribe { (view, entry) ->
                MediaActivity.navigateTo(requireActivity(), entry.id, entry.name, entry.medium.toCategory(), view)
            }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_media_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        searchBottomSheetManager = MediaListSearchBottomSheet.bindTo(this, viewModel)
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
                .autoDispose(this)
                .subscribe {
                    if (it.menuItem().isActionViewExpanded) {
                        searchQuery = null

                        viewModel.reload()
                    }

                    TransitionManager.beginDelayedTransition(toolbar)
                }

            searchView.queryTextChangeEvents()
                .skipInitialValue()
                .autoDispose(this)
                .subscribe {
                    searchQuery = it.queryText().toString().trim()

                    if (it.isSubmitted) {
                        searchView.clearFocus()

                        viewModel.reload()
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

    override fun onBackPressed(): Boolean {
        return searchBottomSheetManager.onBackPressed()
    }

    override fun updateRecyclerViewPadding() {
        super.updateRecyclerViewPadding()

        if (innerAdapter.itemCount > 0 || adapter.footer == null) {
            recyclerView.setPadding(recyclerView.paddingLeft, recyclerView.paddingTop, recyclerView.paddingRight,
                recyclerView.paddingBottom + searchBottomSheetTitle.measuredHeight)
        }
    }
}
