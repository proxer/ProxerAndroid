package me.proxer.app.info.industry

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toCategory
import me.proxer.library.entitiy.list.IndustryProject
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class IndustryProjectFragment : PagedContentFragment<IndustryProject>() {

    companion object {
        fun newInstance() = IndustryProjectFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_projects
    override val isSwipeToRefreshEnabled = false

    override val viewModel: IndustryProjectViewModel by lazy {
        ViewModelProviders.of(this).get(IndustryProjectViewModel::class.java).apply { industryId = id }
    }

    private val industryActivity
        get() = activity as IndustryActivity

    private val id: String
        get() = industryActivity.id

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, VERTICAL)
    }

    override lateinit var innerAdapter: IndustryProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = IndustryProjectAdapter(GlideApp.with(this))
        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { (view, project) ->
                    MediaActivity.navigateTo(activity, project.id, project.name, project.medium.toCategory(),
                            if (view.drawable != null) view else null)
                }
    }
}
