package me.proxer.app.info.translatorgroup

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.list.TranslatorGroupProject
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectFragment : PagedContentFragment<TranslatorGroupProject>() {

    companion object {
        fun newInstance() = TranslatorGroupProjectFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_projects
    override val isSwipeToRefreshEnabled = false

    override val viewModel by unsafeLazy { TranslatorGroupProjectViewModelProvider.get(this, id) }

    override val hostingActivity: TranslatorGroupActivity
        get() = activity as TranslatorGroupActivity

    private val id: String
        get() = hostingActivity.id

    override val layoutManager by unsafeLazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()) + 1, VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<TranslatorGroupProjectAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = TranslatorGroupProjectAdapter()

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, translatorGroup) ->
                MediaActivity.navigateTo(requireActivity(), translatorGroup.id, translatorGroup.name,
                    translatorGroup.medium.toCategory(), view)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }
}
