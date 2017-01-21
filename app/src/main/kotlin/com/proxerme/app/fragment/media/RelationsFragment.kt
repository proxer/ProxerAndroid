package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.media.RelationsAdapter
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.MappedTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.DeviceUtils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.info.entity.Relation
import com.proxerme.library.connection.info.request.RelationRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class RelationsFragment : SingleLoadingFragment<String, Array<Relation>>() {

    companion object {
        fun newInstance(): RelationsFragment {
            return RelationsFragment()
        }
    }

    override val section = Section.RELATIONS

    private val mediaActivity
        get() = activity as MediaActivity

    private lateinit var adapter: RelationsAdapter

    private val id: String
        get() = mediaActivity.id

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = RelationsAdapter()
        adapter.callback = object : RelationsAdapter.RelationsAdapterCallback() {
            override fun onItemClick(item: Relation) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_relations, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        list.adapter = adapter
    }

    override fun onDestroyView() {
        list.layoutManager = null
        list.adapter = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        adapter.removeCallback()

        super.onDestroy()
    }

    override fun present(data: Array<Relation>) {
        if (data.isEmpty()) {
            showError(getString(R.string.error_no_data_relations), null)
        } else {
            adapter.replace(data)
        }
    }

    override fun constructTask(): Task<String, Array<Relation>> {
        return MappedTask(ProxerLoadingTask(::RelationRequest), {
            it.filterNot { it.id == id }.sortedByDescending { it.clicks }.toTypedArray()
        })
    }

    override fun constructInput(): String {
        return id
    }
}