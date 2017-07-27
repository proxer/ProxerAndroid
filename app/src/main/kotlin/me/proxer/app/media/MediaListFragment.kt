package me.proxer.app.media

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.jakewharton.rxbinding2.support.v4.view.actionViewEvents
import com.jakewharton.rxbinding2.support.v7.widget.itemClicks
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.util.DeviceUtils
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

        fun newInstance(category: Category): MediaListFragment {
            return MediaListFragment().apply {
                arguments = bundleOf(CATEGORY_ARGUMENT to category)
            }
        }
    }

    override val viewModel: MediaListViewModel by lazy {
        ViewModelProviders.of(this).get(MediaListViewModel::class.java)
    }

    override val layoutManager by lazy {
        GridLayoutManager(context, DeviceUtils.calculateSpanAmount(activity) + 1)
    }

    override lateinit var innerAdapter: MediaAdapter

    private val category
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as Category

    private val toolbar by lazy { activity.findViewById<Toolbar>(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = MediaAdapter(category, GlideApp.with(this))

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe {
                    // TODO
                }

        toolbar.itemClicks()
                .bindToLifecycle(this)
                .subscribe {
                    when (it.itemId) {
                        R.id.rating -> viewModel.sortCriteria = MediaSearchSortCriteria.RATING
                        R.id.clicks -> viewModel.sortCriteria = MediaSearchSortCriteria.CLICKS
                        R.id.episodeAmount -> viewModel.sortCriteria = MediaSearchSortCriteria.EPISODE_AMOUNT
                        R.id.name -> viewModel.sortCriteria = MediaSearchSortCriteria.NAME
                        R.id.all_anime -> viewModel.type = MediaType.ALL_ANIME
                        R.id.animeseries -> viewModel.type = MediaType.ANIMESERIES
                        R.id.movies -> viewModel.type = MediaType.MOVIE
                        R.id.ova -> viewModel.type = MediaType.OVA
                        R.id.hentai -> viewModel.type = MediaType.HENTAI
                        R.id.all_manga -> viewModel.type = MediaType.ALL_MANGA
                        R.id.mangaseries -> viewModel.type = MediaType.MANGASERIES
                        R.id.oneshot -> viewModel.type = MediaType.ONESHOT
                        R.id.doujin -> viewModel.type = MediaType.DOUJIN
                        R.id.hmanga -> viewModel.type = MediaType.HMANGA
                    }
                }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_media_list, menu, true)

        when (viewModel.sortCriteria) {
            MediaSearchSortCriteria.RATING -> menu.findItem(R.id.rating).isChecked = true
            MediaSearchSortCriteria.CLICKS -> menu.findItem(R.id.clicks).isChecked = true
            MediaSearchSortCriteria.EPISODE_AMOUNT -> menu.findItem(R.id.episodeAmount).isChecked = true
            MediaSearchSortCriteria.NAME -> menu.findItem(R.id.name).isChecked = true
            else -> throw IllegalArgumentException("Unsupported sort criteria: $viewModel.sortCriteria")
        }

        val filterSubMenu = menu.findItem(R.id.filter).subMenu

        when (category) {
            Category.ANIME -> filterSubMenu.setGroupVisible(R.id.filterManga, false)
            Category.MANGA -> filterSubMenu.setGroupVisible(R.id.filterAnime, false)
        }

        when (viewModel.type) {
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
            else -> throw IllegalArgumentException("Unsupported type: $viewModel.type")
        }

        menu.findItem(R.id.search).let {
            it.actionViewEvents()
                    .bindToLifecycle(this)
                    .subscribe {
                        if (!it.menuItem().isActionViewExpanded) {
                            viewModel.searchQuery = null
                        }

                        TransitionManager.beginDelayedTransition(toolbar)
                    }

            (it.actionView as SearchView).queryTextChangeEvents()
                    .bindToLifecycle(this)
                    .subscribe {
                        if (it.isSubmitted) {
                            viewModel.searchQuery = it.queryText().toString()
                        }
                    }
        }
    }
}
