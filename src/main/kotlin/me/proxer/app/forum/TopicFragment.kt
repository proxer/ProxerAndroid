package me.proxer.app.forum

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import me.proxer.app.GlideApp
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
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
                .subscribe { (_, post) ->
                    ProfileActivity.navigateTo(safeActivity, post.userId, post.username)
                }

        viewModel.metaData.observe(this, Observer {
            it?.let { topic = it.subject }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }
}
