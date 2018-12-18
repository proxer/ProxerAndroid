package me.proxer.app.media.discussion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.forum.TopicActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.library.entity.info.ForumDiscussion
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class DiscussionFragment : BaseContentFragment<List<ForumDiscussion>>() {

    companion object {
        fun newInstance() = DiscussionFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<DiscussionViewModel> { parametersOf(id) }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = hostingActivity.id

    private var adapter by Delegates.notNull<DiscussionAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = DiscussionAdapter()

        adapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { item ->
                TopicActivity.navigateTo(requireActivity(), item.id, item.categoryId, item.subject)
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_discussion, container, false)
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun showData(data: List<ForumDiscussion>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_discussions, ACTION_MESSAGE_HIDE))
        }
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
