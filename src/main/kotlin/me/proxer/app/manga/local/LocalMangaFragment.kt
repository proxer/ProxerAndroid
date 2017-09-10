package me.proxer.app.manga.local

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.Button
import android.widget.TextView
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import com.jakewharton.rxbinding2.view.actionViewEvents
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindUntilEvent
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

    override val viewModel: LocalMangaViewModel by unsafeLazy {
        LocalMangaViewModelProvider.get(this, searchQuery)
    }

    private var adapter by Delegates.notNull<LocalMangaAdapter>()

    private var searchQuery: String?
        get() = arguments.getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            arguments.putString(SEARCH_QUERY_ARGUMENT, value)

            viewModel.searchQuery = value
        }

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val toolbar by unsafeLazy { activity.findViewById<Toolbar>(R.id.toolbar) }
    private val jobInfoContainer: ViewGroup by bindView(R.id.jobInfoContainer)
    private val jobInfoText: TextView by bindView(R.id.jobInfoText)
    private val jobInfoCancel: Button by bindView(R.id.jobInfoCancel)
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = LocalMangaAdapter(savedInstanceState)

        adapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { (entry, chapter) ->
                    MangaActivity.navigateTo(activity, entry.id, chapter.episode, chapter.language, chapter.title,
                            entry.name, entry.episodeAmount)
                }

        adapter.longClickSubject
                .bindToLifecycle(this)
                .subscribe { (view, entry) ->
                    MediaActivity.navigateTo(activity, entry.id, entry.name, Category.MANGA,
                            if (view.drawable != null) view else null)
                }

        adapter.deleteClickSubject
                .subscribeOn(Schedulers.io())
                .bindToLifecycle(this)
                .subscribe { (_, chapter) -> viewModel.deleteChapter(chapter) }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_local_manga, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter

        jobInfoCancel.clicks()
                .observeOn(Schedulers.io())
                .bindToLifecycle(this)
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

                recyclerView.setPadding(recyclerView.paddingLeft, DeviceUtils.getVerticalMargin(context),
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

        when {
            data.isEmpty() -> {
                adapter.clearAndNotifyRemoval()

                if (searchQuery.isNullOrBlank()) {
                    showError(ErrorAction(R.string.error_no_data_local_manga, ACTION_MESSAGE_HIDE))
                } else {
                    showError(ErrorAction(R.string.error_no_data_search, ACTION_MESSAGE_HIDE))
                }
            }
            else -> adapter.swapDataAndNotifyChange(data)
        }
    }

    override fun hideData() = Unit

    override fun showError(action: ErrorAction) {
        contentContainer.visibility = View.GONE

        super.showError(action)
    }

    override fun hideError() = Unit
}
