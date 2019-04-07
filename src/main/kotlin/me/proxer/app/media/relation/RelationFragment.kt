package me.proxer.app.media.relation

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.library.entity.info.Relation
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class RelationFragment : BaseContentFragment<List<Relation>>(R.layout.fragment_relation) {

    companion object {
        fun newInstance() = RelationFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<RelationViewModel> { parametersOf(id) }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = hostingActivity.id

    private var adapter by Delegates.notNull<RelationAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = RelationAdapter()

        adapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, relation) ->
                MediaActivity.navigateTo(requireActivity(), relation.id, relation.name, relation.category, view)
            }
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = StaggeredGridLayoutManager(
            DeviceUtils.calculateSpanAmount(requireActivity()) + 1,
            StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = adapter
    }

    override fun showData(data: List<Relation>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_relations, ACTION_MESSAGE_HIDE))
        }
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
