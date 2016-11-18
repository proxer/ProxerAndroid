package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.media.RelationsAdapter
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.info.entity.Relation
import com.proxerme.library.connection.info.request.RelationRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class RelationsFragment : EasyLoadingFragment<Array<Relation>>() {

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

    private lateinit var adapter: RelationsAdapter
    override var result: Array<Relation>?
        get() {
            return adapter.items.toTypedArray()
        }
        set(value) {
            if (value == null) {
                adapter.clear()
            } else {
                adapter.replace(value)
            }
        }

    private lateinit var id: String

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments.getString(ARGUMENT_ID)

        adapter = RelationsAdapter(savedInstanceState)
        adapter.callback = object : RelationsAdapter.RelationsAdapterCallback() {
            override fun onItemClick(item: Relation) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }
    }

    override fun onDestroy() {
        adapter.callback = null

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_relations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun showContent(result: Array<Relation>) {
        // Nothing to do
    }

    override fun constructLoadingRequest(): LoadingRequest<Array<Relation>> {
        return LoadingRequest(RelationRequest(id))
    }
}