package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.media.CommentAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.LoadingTask
import com.proxerme.app.task.Task
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.connection.info.request.CommentRequest
import com.proxerme.library.parameters.CommentSortParameter
import com.proxerme.library.parameters.CommentSortParameter.CommentSort

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CommentFragment : PagedLoadingFragment<Comment>() {

    companion object {

        private const val ARGUMENT_ID = "id"

        fun newInstance(id: String): CommentFragment {
            return CommentFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.COMMENTS
    override val itemsOnPage = 15

    @CommentSort
    private var sortCriteria = CommentSortParameter.RATING

    override lateinit var adapter: CommentAdapter
    override lateinit var layoutManager: LinearLayoutManager

    private val id: String
        get() = arguments.getString(ARGUMENT_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = CommentAdapter()

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

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.callback = object : CommentAdapter.CommentAdapterCallback() {
            override fun onUserClick(item: Comment) {
                UserActivity.navigateTo(activity, item.userId, item.username, item.imageId)
            }
        }
    }

    override fun constructTask(pageCallback: () -> Int): Task<Array<Comment>> {
        return LoadingTask {
            CommentRequest(id)
                    .withPage(pageCallback.invoke())
                    .withLimit(itemsOnPage)
                    .withSortType(sortCriteria)
        }
    }
}