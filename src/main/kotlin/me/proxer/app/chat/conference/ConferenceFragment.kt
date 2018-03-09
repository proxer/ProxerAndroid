package me.proxer.app.chat.conference

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import io.reactivex.disposables.Disposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.chat.ChatActivity
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.create.CreateChatActivity
import me.proxer.app.chat.sync.ChatNotifications
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.isAtTop
import me.proxer.app.util.extension.postDelayedSafely
import me.proxer.app.util.extension.scrollToTop
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ConferenceFragment : BaseContentFragment<List<LocalConference>>() {

    companion object {
        fun newInstance() = ConferenceFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { ConferenceViewModelProvider.get(this) }

    private var adapter by Delegates.notNull<ConferenceAdapter>()

    private var pingDisposable: Disposable? = null

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    private var isFirstData = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceAdapter()

        adapter.clickSubject
            .autoDispose(this)
            .subscribe { ChatActivity.navigateTo(requireActivity(), it) }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conferences, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()),
            StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        ChatNotifications.cancel(requireContext())

        pingDisposable = bus.register(ConferenceFragmentPingEvent::class.java).subscribe()
    }

    override fun onPause() {
        pingDisposable?.dispose()
        pingDisposable = null

        super.onPause()
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_conferences, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create_chat -> CreateChatActivity.navigateTo(requireActivity(), false)
            R.id.new_group -> CreateChatActivity.navigateTo(requireActivity(), true)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showData(data: List<LocalConference>) {
        super.showData(data)

        val wasAtFirstPosition = recyclerView.layoutManager.isAtTop()
        val wasEmpty = adapter.isEmpty()

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_conferences, ACTION_MESSAGE_HIDE))
        } else if (!isFirstData && (wasAtFirstPosition || wasEmpty)) {
            recyclerView.postDelayedSafely({ recyclerView ->
                when {
                    wasEmpty -> recyclerView.layoutManager.scrollToTop()
                    else -> recyclerView.smoothScrollToPosition(0)
                }
            }, 50)
        }

        isFirstData = false
    }

    override fun hideData() {
        super.hideData()

        adapter.swapDataAndNotifyWithDiffing(emptyList())
    }

    override fun showError(action: ErrorAction) {
        super.showError(action)

        isFirstData = false
    }
}
