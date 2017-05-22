package me.proxer.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.media.RelationsAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.bindView
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.Relation
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class RelationsFragment : LoadingFragment<ProxerCall<List<Relation>>, List<Relation>>() {

    companion object {
        fun newInstance(): RelationsFragment {
            return RelationsFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    private val mediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = mediaActivity.id

    private val adapter by lazy { RelationsAdapter(GlideApp.with(this)) }

    private val list: RecyclerView by bindView(R.id.list)

    override fun onDestroy() {
        adapter.destroy()

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_relations, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.callback = object : RelationsAdapter.RelationsAdapterCallback {
            override fun onRelationClick(view: View, item: Relation) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, item.id, item.name, item.category,
                        if (imageView.drawable != null) imageView else null)
            }
        }

        list.setHasFixedSize(true)
        list.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        list.adapter = adapter
    }

    override fun onSuccess(result: List<Relation>) {
        adapter.replace(result)

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (adapter.isEmpty()) {
            showError(R.string.error_no_data_relations, ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    override fun constructInput() = api.info().relations(id).build()
    override fun constructTask() = TaskBuilder.asyncProxerTask<List<Relation>>()
            .map { it.filterNot { it.id == id }.sortedByDescending { it.clicks } }
            .build()
}