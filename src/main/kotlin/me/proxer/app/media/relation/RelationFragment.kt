package me.proxer.app.media.relation

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.Relation
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class RelationFragment : BaseContentFragment<List<Relation>>() {

    companion object {
        fun newInstance() = RelationFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { RelationViewModelProvider.get(this, id) }

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
                .bindToLifecycle(this)
                .subscribe { (view, relation) ->
                    MediaActivity.navigateTo(activity, relation.id, relation.name, relation.category,
                            if (view.drawable != null) view else null)
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_relation, container, false)
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter
    }

    override fun showData(data: List<Relation>) {
        super.showData(data)

        adapter.swapDataAndNotifyInsertion(data)

        if (adapter.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_relations, ACTION_MESSAGE_HIDE))
        }
    }

    override fun hideData() {
        adapter.clearAndNotifyRemoval()

        super.hideData()
    }
}
