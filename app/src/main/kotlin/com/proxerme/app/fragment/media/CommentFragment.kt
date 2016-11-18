package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.proxerme.app.R
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.media.CommentAdapter
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.connection.info.request.CommentRequest
import com.proxerme.library.parameters.CommentSortParameter
import com.proxerme.library.parameters.CommentSortParameter.CommentSort

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CommentFragment : EasyPagingFragment<Comment>() {

    companion object {

        const val ITEMS_ON_PAGE = 15

        private const val ARGUMENT_ID = "id"
        private const val STATE_SORT_CRITERIA = "state_sort_criteria"

        fun newInstance(id: String): CommentFragment {
            return CommentFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.COMMENTS
    override val itemsOnPage = ITEMS_ON_PAGE

    @CommentSort
    private lateinit var sortCriteria: String

    override lateinit var adapter: CommentAdapter
    override lateinit var layoutManager: LinearLayoutManager

    private lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments.getString(ARGUMENT_ID)

        if (savedInstanceState != null) {
            sortCriteria = savedInstanceState.getString(STATE_SORT_CRITERIA)
        } else {
            sortCriteria = CommentSortParameter.RATING
        }

        adapter = CommentAdapter(savedInstanceState)
        adapter.callback = object : CommentAdapter.CommentAdapterCallback() {
            override fun onUserClick(item: Comment) {
                UserActivity.navigateTo(activity, item.userId, item.username, item.imageId)
            }
        }

        layoutManager = LinearLayoutManager(context)

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
        outState.putString(STATE_SORT_CRITERIA, sortCriteria)
    }

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<Comment>> {
        return LoadingRequest(CommentRequest(id)
                .withPage(page)
                .withLimit(itemsOnPage)
                .withSortType(sortCriteria))
    }
}