package me.proxer.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.mikepenz.iconics.utils.IconicsMenuInflatorUtil
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.adapter.media.CommentAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.Comment
import me.proxer.library.enums.Category
import me.proxer.library.enums.CommentSortCriteria
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class CommentsFragment : PagedLoadingFragment<ProxerCall<List<Comment>>, Comment>() {

    companion object {
        private const val SORT_CRITERIA_ARGUMENT = "sort_criteria"

        fun newInstance(): CommentsFragment {
            return CommentsFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val emptyResultMessage = R.string.error_no_data_comments
    override val isSwipeToRefreshEnabled = true
    override val pagingThreshold = 3
    override val itemsOnPage = 10

    private val mediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = mediaActivity.id

    private val category: Category?
        get() = mediaActivity.category

    private var sortCriteria: CommentSortCriteria
        get() = arguments.getSerializable(SORT_CRITERIA_ARGUMENT) as? CommentSortCriteria
                ?: CommentSortCriteria.RATING
        set(value) = arguments.putSerializable(SORT_CRITERIA_ARGUMENT, value)

    override val layoutManager by lazy { LinearLayoutManager(context) }
    override lateinit var innerAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = CommentAdapter(savedInstanceState, { category ?: Category.ANIME }, GlideApp.with(this))

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.callback = object : CommentAdapter.CommentAdapterCallback {
            override fun onUserClick(view: View, item: Comment) {
                val imageView = view.find<ImageView>(R.id.userImage)

                ProfileActivity.navigateTo(activity, item.authorId, item.author, item.image,
                        if (imageView.drawable != null && item.image.isNotBlank()) imageView else null)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflatorUtil.inflate(inflater, context, R.menu.fragment_comments, menu, true)

        when (sortCriteria) {
            CommentSortCriteria.RATING -> menu.findItem(R.id.rating).isChecked = true
            CommentSortCriteria.TIME -> menu.findItem(R.id.time).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCriteria = sortCriteria

        when (item.itemId) {
            R.id.rating -> sortCriteria = CommentSortCriteria.RATING
            R.id.time -> sortCriteria = CommentSortCriteria.TIME
            else -> return false
        }

        if (sortCriteria != previousCriteria) {
            freshLoad()

            item.isChecked = true
        }

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<Comment>>().build()
    override fun constructPagedInput(page: Int) = api.info().comments(id)
            .page(page)
            .limit(itemsOnPage)
            .sort(sortCriteria)
            .build()
}
