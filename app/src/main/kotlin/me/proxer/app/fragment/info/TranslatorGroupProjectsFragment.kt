package me.proxer.app.fragment.info

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.TranslatorGroupActivity
import me.proxer.app.adapter.info.TranslatorGroupProjectAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.extension.toCategory
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.list.TranslatorGroupProject
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectsFragment : PagedLoadingFragment<ProxerCall<List<TranslatorGroupProject>>,
        TranslatorGroupProject>() {

    companion object {
        fun newInstance(): TranslatorGroupProjectsFragment {
            return TranslatorGroupProjectsFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val itemsOnPage = 30
    override val emptyResultMessage = R.string.error_no_data_projects
    override val spanCount get() = super.spanCount + 1

    private val translatorGroupActivity
        get() = activity as TranslatorGroupActivity

    private val id: String
        get() = translatorGroupActivity.id

    override val innerAdapter by lazy { TranslatorGroupProjectAdapter(GlideApp.with(this)) }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.callback = object : TranslatorGroupProjectAdapter.TranslatorGroupProjectAdapterCallback {
            override fun onProjectClick(view: View, item: TranslatorGroupProject) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, item.id, item.name, item.medium.toCategory(),
                        if (imageView.drawable != null) imageView else null)
            }
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<TranslatorGroupProject>>().build()
    override fun constructPagedInput(page: Int) = api.list().translatorGroupProjectList(id)
            .includeHentai(PreferenceHelper.isAgeRestrictedMediaAllowed(context) && StorageHelper.user != null)
            .page(page)
            .limit(itemsOnPage)
            .build()
}
