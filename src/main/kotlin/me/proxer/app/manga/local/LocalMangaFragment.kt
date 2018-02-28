package me.proxer.app.manga.local

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import com.jakewharton.rxbinding2.view.actionViewEvents
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import io.reactivex.schedulers.Schedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class LocalMangaFragment : BaseContentFragment<List<CompleteLocalMangaEntry>>() {

    companion object {
        private const val SEARCH_QUERY_ARGUMENT = "search_query"

        fun newInstance() = LocalMangaFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { LocalMangaViewModelProvider.get(this, searchQuery) }

    private var adapter by Delegates.notNull<LocalMangaAdapter>()

    private var searchQuery: String?
        get() = requireArguments().getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            requireArguments().putString(SEARCH_QUERY_ARGUMENT, value)

            viewModel.searchQuery = value
        }

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }
    private val jobInfoContainer: ViewGroup by bindView(R.id.jobInfoContainer)
    private val jobInfoText: TextView by bindView(R.id.jobInfoText)
    private val jobInfoCancel: Button by bindView(R.id.jobInfoCancel)
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = LocalMangaAdapter(savedInstanceState)

        adapter.clickSubject
                .autoDispose(this)
                .subscribe { (entry, chapter) ->
                    MangaActivity.navigateTo(requireActivity(), entry.id, chapter.episode, chapter.language,
                            chapter.title, entry.name, entry.episodeAmount)
                }

        adapter.longClickSubject
                .autoDispose(this)
                .subscribe { (view, entry) ->
                    MediaActivity.navigateTo(requireActivity(), entry.id, entry.name, Category.MANGA,
                            if (view.drawable != null) view else null)
                }

        adapter.deleteClickSubject
                .subscribeOn(Schedulers.io())
                .autoDispose(this)
                .subscribe { (_, chapter) -> viewModel.deleteChapter(chapter) }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_local_manga, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter

        jobInfoCancel.clicks()
                .observeOn(Schedulers.io())
                .autoDispose(this)
                .subscribe {
                    LocalMangaJob.cancelAll()

                    viewModel.updateJobInfo()
                }

        viewModel.jobInfo.observe(this, Observer {
            if (it != null) {
                jobInfoContainer.visibility = View.VISIBLE
                jobInfoText.text = it

                recyclerView.setPadding(recyclerView.paddingLeft, 0, recyclerView.paddingRight,
                        recyclerView.paddingBottom)
            } else {
                jobInfoContainer.visibility = View.GONE

                recyclerView.setPadding(recyclerView.paddingLeft, DeviceUtils.getVerticalMargin(requireContext()),
                        recyclerView.paddingRight, recyclerView.paddingBottom)
            }
        })
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_local_manga, menu, true)

        menu.findItem(R.id.search).let { searchItem ->
            val searchView = searchItem.actionView as SearchView

            searchItem.actionViewEvents()
                    .autoDispose(this)
                    .subscribe {
                        if (it.menuItem().isActionViewExpanded) {
                            searchQuery = null
                        }

                        TransitionManager.beginDelayedTransition(toolbar)
                    }

            searchView.queryTextChangeEvents()
                    .skipInitialValue()
                    .autoDispose(this)
                    .subscribe {
                        searchQuery = it.queryText().toString()

                        if (it.isSubmitted) {
                            searchView.clearFocus()
                        }
                    }

            searchQuery?.let {
                searchItem.expandActionView()
                searchView.setQuery(it, false)
                searchView.clearFocus()
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun showData(data: List<CompleteLocalMangaEntry>) {
        super.showData(data)

        errorContainer.visibility = View.GONE

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            if (searchQuery.isNullOrBlank()) {
                showError(ErrorAction(R.string.error_no_data_local_manga, ACTION_MESSAGE_HIDE))
            } else {
                showError(ErrorAction(R.string.error_no_data_search, ACTION_MESSAGE_HIDE))
            }
        }
    }

    override fun hideData() {}

    override fun showError(action: ErrorAction) {
        contentContainer.visibility = View.GONE

        super.showError(action)
    }

    override fun hideError() {}
}
