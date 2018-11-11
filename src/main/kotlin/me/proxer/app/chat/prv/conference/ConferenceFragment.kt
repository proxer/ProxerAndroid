package me.proxer.app.chat.prv.conference

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.TransitionManager
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import com.jakewharton.rxbinding2.view.actionViewEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.newbase.paged.NewBasePagedFragment
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class ConferenceFragment : NewBasePagedFragment<ConferenceWithMessage>() {

    companion object {
        private const val SEARCH_QUERY_ARGUMENT = "search_query"

        fun newInstance() = ConferenceFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = false

    override val emptyDataMessage: Int
        get() = when (searchQuery.isNullOrBlank()) {
            true -> R.string.error_no_data_conferences
            false -> R.string.error_no_data_search
        }

    override val viewModel by viewModel<ConferenceViewModel> { parametersOf(searchQuery ?: "") }

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(
            DeviceUtils.calculateSpanAmount(requireActivity()),
            StaggeredGridLayoutManager.VERTICAL
        )
    }

    override val innerAdapter = ConferenceAdapter(storageHelper)

    private var searchQuery: String?
        get() = requireArguments().getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            requireArguments().putString(SEARCH_QUERY_ARGUMENT, value)

            viewModel.searchQuery = value ?: ""
        }

    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { (conference) -> MessengerActivity.navigateTo(requireActivity(), conference) }

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()

        if (!MessengerWorker.isRunning()) {
            MessengerWorker.enqueueSynchronization()
        }
    }

    override fun onResume() {
        super.onResume()

        MessengerNotifications.cancel(requireContext())

        bus.register(ConferenceFragmentPingEvent::class.java)
            .autoDisposable(this.scope())
            .subscribe()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_conferences, menu, true)

        menu.findItem(R.id.search).let { searchItem ->
            val searchView = searchItem.actionView as SearchView

            searchItem.actionViewEvents()
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe {
                    if (it.menuItem().isActionViewExpanded) {
                        searchQuery = null
                    }

                    TransitionManager.beginDelayedTransition(toolbar)
                }

            searchView.queryTextChangeEvents()
                .skipInitialValue()
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe {
                    searchQuery = it.queryText().toString().trim()

                    if (it.isSubmitted) {
                        searchView.clearFocus()
                    }
                }

            searchQuery?.let {
                if (it.isNotBlank()) {
                    searchItem.expandActionView()
                    searchView.setQuery(it, false)
                    searchView.clearFocus()
                }
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create_chat -> CreateConferenceActivity.navigateTo(requireActivity(), false)
            R.id.new_group -> CreateConferenceActivity.navigateTo(requireActivity(), true)
        }

        return super.onOptionsItemSelected(item)
    }
}
