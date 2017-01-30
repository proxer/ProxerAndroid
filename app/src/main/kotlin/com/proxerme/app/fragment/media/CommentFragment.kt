package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.adapter.media.CommentAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.fragment.media.CommentFragment.CommentInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.connection.info.request.CommentRequest
import com.proxerme.library.parameters.CommentSortParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CommentFragment : PagedLoadingFragment<CommentInput, Comment>() {

    companion object {
        fun newInstance(): CommentFragment {
            return CommentFragment()
        }
    }

    override val section = Section.COMMENTS
    override val itemsOnPage = 15

    private val mediaActivity
        get() = activity as MediaActivity

    private var sortCriteria = CommentSortParameter.RATING

    override lateinit var adapter: CommentAdapter
    override lateinit var layoutManager: LinearLayoutManager

    private val id: String
        get() = mediaActivity.id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = CommentAdapter()
        adapter.callback = object : CommentAdapter.CommentAdapterCallback() {
            override fun onUserClick(item: Comment) {
                ProfileActivity.navigateTo(activity, item.userId, item.username, item.imageId)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_comments, menu)

        when (sortCriteria) {
            CommentSortParameter.RATING -> menu.findItem(R.id.rating).isChecked = true
            CommentSortParameter.NEWEST -> menu.findItem(R.id.time).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCriteria = sortCriteria

        when (item.itemId) {
            R.id.rating -> sortCriteria = CommentSortParameter.RATING
            R.id.time -> sortCriteria = CommentSortParameter.NEWEST
            else -> return false
        }

        if (sortCriteria != previousCriteria) {
            reset()

            item.isChecked = true
        }

        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        layoutManager = LinearLayoutManager(context)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun constructTask(): Task<CommentInput, Array<Comment>> {
        return ProxerLoadingTask({
            CommentRequest(it.id)
                    .withPage(it.page)
                    .withLimit(it.itemsOnPage)
                    .withSortType(it.sortCriteria)
        })
    }

    override fun constructInput(page: Int): CommentInput {
        return CommentInput(page, id, itemsOnPage, sortCriteria)
    }

    override fun getEmptyMessage(): String {
        return getString(R.string.error_no_data_comments)
    }

    class CommentInput(page: Int, val id: String, val itemsOnPage: Int, val sortCriteria: String) :
            PagedInput(page)
}