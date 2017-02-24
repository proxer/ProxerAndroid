package com.proxerme.app.fragment.info

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.activity.TranslatorGroupActivity
import com.proxerme.app.adapter.info.ProjectAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.fragment.info.TranslatorGroupProjectsFragment.TranslatorGroupProjectsInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.DeviceUtils
import com.proxerme.app.util.ParameterMapper
import com.proxerme.library.connection.list.entity.ProjectListEntry
import com.proxerme.library.connection.list.request.TranslatorGroupProjectsRequest
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class TranslatorGroupProjectsFragment : PagedLoadingFragment<TranslatorGroupProjectsInput, ProjectListEntry>() {

    companion object {
        fun newInstance(): TranslatorGroupProjectsFragment {
            return TranslatorGroupProjectsFragment()
        }
    }

    override val section = Section.INDUSTRY_PROJECTS
    override val itemsOnPage = 30

    private val translatorGroupActivity
        get() = activity as TranslatorGroupActivity

    private val id
        get() = translatorGroupActivity.id

    override lateinit var layoutManager: StaggeredGridLayoutManager
    override lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ProjectAdapter()
        adapter.callback = object : ProjectAdapter.ProjectAdapterCallback() {
            override fun onItemClick(item: ProjectListEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name,
                        ParameterMapper.mediumToCategory(item.medium) ?: CategoryParameter.ANIME)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun constructTask(): Task<TranslatorGroupProjectsInput, Array<ProjectListEntry>> {
        return ProxerLoadingTask({
            TranslatorGroupProjectsRequest(it.id, it.page).withLimit(itemsOnPage)
        })
    }

    override fun constructInput(page: Int): TranslatorGroupProjectsInput {
        return TranslatorGroupProjectsInput(page, id)
    }

    override fun getEmptyMessage(): Int {
        return R.string.error_no_data_projects
    }

    class TranslatorGroupProjectsInput(page: Int, val id: String) : PagedInput(page)
}
