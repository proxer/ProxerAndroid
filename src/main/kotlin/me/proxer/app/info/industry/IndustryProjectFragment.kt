package me.proxer.app.info.industry

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import android.view.View
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.list.IndustryProject
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

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

    override val viewModel: IndustryProjectViewModel by unsafeLazy { IndustryProjectViewModelProvider.get(this, id) }

    private val industryActivity
        get() = activity as IndustryActivity

    private val id: String
        get() = industryActivity.id

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<IndustryProjectAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = IndustryProjectAdapter()

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { (view, project) ->
                    MediaActivity.navigateTo(activity, project.id, project.name, project.medium.toCategory(),
                            if (view.drawable != null) view else null)
                }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }
}
