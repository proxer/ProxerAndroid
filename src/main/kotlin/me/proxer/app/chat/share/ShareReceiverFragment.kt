package me.proxer.app.chat.share

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.TransitionManager
import com.jakewharton.rxbinding3.appcompat.queryTextChangeEvents
import com.jakewharton.rxbinding3.view.actionViewEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.conference.ConferenceAdapter
import me.proxer.app.chat.prv.conference.ConferenceViewModel
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ShareReceiverFragment : BaseContentFragment<List<ConferenceWithMessage>>(R.layout.fragment_conferences) {

    companion object {
        private const val SEARCH_QUERY_ARGUMENT = "search_query"

        fun newInstance() = ShareReceiverFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<ConferenceViewModel> { parametersOf(searchQuery ?: "") }

    override val hostingActivity: ShareReceiverActivity
        get() = activity as ShareReceiverActivity

    private val text: String
        get() = hostingActivity.text

    private var adapter by Delegates.notNull<ConferenceAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    private var searchQuery: String?
        get() = requireArguments().getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            requireArguments().putString(SEARCH_QUERY_ARGUMENT, value)

            viewModel.searchQuery = value ?: ""
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceAdapter(storageHelper)

        adapter.clickSubject
            .debounce(50, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(this.scope())
            .subscribe { (conference) ->
                MessengerActivity.navigateTo(requireActivity(), conference, text)

                requireActivity().finish()
            }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = StaggeredGridLayoutManager(
            DeviceUtils.calculateSpanAmount(requireActivity()),
            StaggeredGridLayoutManager.VERTICAL
        )

        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_share_receiver, menu, true)

        menu.findItem(R.id.search).let { searchItem ->
            val searchView = searchItem.actionView as SearchView

            searchItem.actionViewEvents()
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe {
                    if (it.menuItem.isActionViewExpanded) {
                        searchQuery = null
                    }

                    TransitionManager.endTransitions(toolbar)
                    TransitionManager.beginDelayedTransition(toolbar)
                }

            searchView.queryTextChangeEvents()
                .skipInitialValue()
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe {
                    searchQuery = it.queryText.toString().trim()

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

    override fun showData(data: List<ConferenceWithMessage>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            if (searchQuery.isNullOrBlank()) {
                showError(ErrorAction(R.string.error_no_data_conferences, ACTION_MESSAGE_HIDE))
            } else {
                showError(ErrorAction(R.string.error_no_data_search, ACTION_MESSAGE_HIDE))
            }
        }
    }

    override fun hideData() {
        super.hideData()

        adapter.swapDataAndNotifyWithDiffing(emptyList())
    }
}
