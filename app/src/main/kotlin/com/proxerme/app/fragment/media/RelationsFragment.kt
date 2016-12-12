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
import com.proxerme.app.task.framework.ListenableTask
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

        private const val ARGUMENT_ID = "id"

        fun newInstance(id: String): RelationsFragment {
            return RelationsFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.RELATIONS

    private val id: String
        get() = arguments.getString(ARGUMENT_ID)

    private lateinit var adapter: RelationsAdapter

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
        adapter.callback = null

        super.onDestroy()
    }

    override fun present(data: Array<Relation>) {
        adapter.replace(data)
    }

    override fun constructTask(): ListenableTask<String, Array<Relation>> {
        return ProxerLoadingTask(::RelationRequest)
    }

    override fun constructInput(): String {
        return id
    }
}