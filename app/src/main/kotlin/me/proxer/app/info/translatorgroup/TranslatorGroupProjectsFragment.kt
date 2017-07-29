package me.proxer.app.info.translatorgroup

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import android.view.View
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.info.TranslatorGroupProjectAdapter
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toCategory
import me.proxer.library.entitiy.list.TranslatorGroupProject
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectsFragment : PagedContentFragment<TranslatorGroupProject>() {

    companion object {
        fun newInstance() = TranslatorGroupProjectsFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_projects

    override val viewModel: TranslatorGroupProjectViewModel by lazy {
        ViewModelProviders.of(this).get(TranslatorGroupProjectViewModel::class.java)
                .apply { translatorGroupId = id }
    }

    override val hostingActivity: TranslatorGroupActivity
        get() = activity as TranslatorGroupActivity

    private val id: String
        get() = hostingActivity.id

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, VERTICAL)
    }

    override lateinit var innerAdapter: TranslatorGroupProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = TranslatorGroupProjectAdapter(GlideApp.with(this))
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe { (view, translatorGroup) ->
                    MediaActivity.navigateTo(activity, translatorGroup.id, translatorGroup.name,
                            translatorGroup.medium.toCategory(), if (view.drawable != null) view else null)
                }
    }
}
