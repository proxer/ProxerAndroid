package me.proxer.app.forum

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class TopicFragment : PagedContentFragment<ParsedPost>() {

    companion object {
        fun newInstance() = TopicFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = false

    override val hostingActivity: TopicActivity
        get() = activity as TopicActivity

    override val viewModel by viewModel<TopicViewModel> { parametersOf(id, resources) }

    override val layoutManager by unsafeLazy { LinearLayoutManager(context) }

    override var innerAdapter by Delegates.notNull<PostAdapter>()

    private val id: String
        get() = hostingActivity.id

    private var topic: String?
        get() = hostingActivity.topic
        set(value) {
            hostingActivity.topic = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = PostAdapter()

        innerAdapter.profileClickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, post) ->
                ProfileActivity.navigateTo(
                    requireActivity(), post.userId, post.username, post.image,
                    if (view.drawable != null && post.image.isNotBlank()) view else null
                )
            }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        viewModel.metaData.observe(viewLifecycleOwner, Observer {
            it?.let { topic = it.subject }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_topic, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_in_browser -> {
                val url = (activity?.intent?.dataString ?: "").toPrefixedUrlOrNull()

                if (url != null) {
                    val mobileUrl = url.newBuilder()
                        .setQueryParameter("device", ProxerUtils.getSafeApiEnumName(Device.MOBILE))
                        .build()

                    showPage(mobileUrl, forceBrowser = true, skipCheck = true)
                } else {
                    viewModel.metaData.value?.categoryId?.also { categoryId ->
                        showPage(
                            ProxerUrls.forumWeb(categoryId, id, Device.MOBILE),
                            forceBrowser = true,
                            skipCheck = true
                        )
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
