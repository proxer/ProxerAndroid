package me.proxer.app.fragment.info

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.IndustryActivity
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.info.IndustryProjectAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.extension.toCategory
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.list.IndustryProject
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class IndustryProjectsFragment : PagedLoadingFragment<ProxerCall<List<IndustryProject>>, IndustryProject>() {

    companion object {
        fun newInstance(): IndustryProjectsFragment {
            return IndustryProjectsFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val itemsOnPage = 30
    override val emptyResultMessage = R.string.error_no_data_projects
    override val spanCount get() = super.spanCount + 1

    private val industryActivity
        get() = activity as IndustryActivity

    private val id: String
        get() = industryActivity.id

    override val innerAdapter = IndustryProjectAdapter()

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.callback = object : IndustryProjectAdapter.IndustryProjectAdapterCallback {
            override fun onProjectClick(view: View, item: IndustryProject) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, item.id, item.name, item.medium.toCategory(),
                        if (imageView.drawable != null) imageView else null)
            }
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<IndustryProject>>().build()
    override fun constructPagedInput(page: Int) = api.list().industryProjectList(id)
            .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(context) && StorageHelper.loginToken != null)
            .page(page)
            .limit(itemsOnPage)
            .build()
}
