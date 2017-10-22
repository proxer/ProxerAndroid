package me.proxer.app.chat.share

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.chat.ChatActivity
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.conference.ConferenceAdapter
import me.proxer.app.chat.conference.ConferenceViewModelProvider
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.bundleOf
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ShareReceiverFragment : BaseContentFragment<List<LocalConference>>() {

    companion object {
        fun newInstance() = ShareReceiverFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { ConferenceViewModelProvider.get(this) }

    override val hostingActivity: ShareReceiverActivity
        get() = activity as ShareReceiverActivity

    private val text: String
        get() = hostingActivity.text

    private var adapter by Delegates.notNull<ConferenceAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceAdapter()

        adapter.clickSubject
                .debounce(50, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this)
                .subscribe {
                    ChatActivity.navigateTo(activity, it, text)

                    activity.finish()
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conferences, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun showData(data: List<LocalConference>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_conferences, ACTION_MESSAGE_HIDE))
        }
    }

    override fun hideData() {
        super.hideData()

        adapter.swapDataAndNotifyWithDiffing(emptyList())
    }
}
