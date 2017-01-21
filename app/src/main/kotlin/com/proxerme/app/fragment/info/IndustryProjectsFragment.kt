package com.proxerme.app.fragment.info

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.info.ProjectAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.fragment.info.IndustryProjectsFragment.IndustryProjectsInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.DeviceUtils
import com.proxerme.library.connection.list.entity.ProjectListEntry
import com.proxerme.library.connection.list.request.IndustryProjectsRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class IndustryProjectsFragment : PagedLoadingFragment<IndustryProjectsInput, ProjectListEntry>() {

    companion object {
        fun newInstance(): IndustryProjectsFragment {
            return IndustryProjectsFragment()
        }
    }

    override val section = Section.INDUSTRY_PROJECTS
    override val isSwipeToRefreshEnabled = false
    override val itemsOnPage = 30

    private val industryActivity
        get() = activity as com.proxerme.app.activity.IndustryActivity

    private val id
        get() = industryActivity.id

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ProjectAdapter()
        adapter.callback = object : ProjectAdapter.ProjectAdapterCallback() {
            override fun onItemClick(item: ProjectListEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun constructTask(): Task<IndustryProjectsInput, Array<ProjectListEntry>> {
        return ProxerLoadingTask({ IndustryProjectsRequest(it.id, it.page).withLimit(itemsOnPage) })
    }

    override fun constructInput(page: Int): IndustryProjectsInput {
        return IndustryProjectsInput(page, id)
    }

    class IndustryProjectsInput(page: Int, val id: String) : PagedInput(page)
}