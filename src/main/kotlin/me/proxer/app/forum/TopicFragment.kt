package me.proxer.app.forum

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf
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

    override val viewModel by unsafeLazy { TopicViewModelProvider.get(this, id) }

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
            .autoDispose(this)
            .subscribe { (view, post) ->
                ProfileActivity.navigateTo(requireActivity(), post.userId, post.username, post.image,
                    if (view.drawable != null && post.image.isNotBlank()) view else null)
            }

        viewModel.metaData.observe(this, Observer {
            it?.let { topic = it.subject }
        })

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_topic, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_in_browser -> {
                val url = HttpUrl.parse(activity?.intent?.dataString ?: "")

                if (url != null) {
                    val mobileUrl = url.newBuilder()
                        .setQueryParameter("device", ProxerUtils.getApiEnumName(Device.MOBILE))
                        .build()

                    showPage(mobileUrl, true)
                } else {
                    viewModel.metaData.value?.categoryId?.also { categoryId ->
                        showPage(ProxerUrls.forumWeb(categoryId, id, Device.MOBILE), true)
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
