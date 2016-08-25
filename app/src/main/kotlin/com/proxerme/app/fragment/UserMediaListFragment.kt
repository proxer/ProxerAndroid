package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.adapter.UserMediaAdapter
import com.proxerme.app.fragment.framework.PagingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.user.entitiy.UserMediaListEntry
import com.proxerme.library.connection.user.request.UserMediaListRequest
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.UserMediaSortParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UserMediaListFragment : PagingFragment<UserMediaListEntry>() {

    companion object {

        const val ITEMS_ON_PAGE = 30

        private const val ARGUMENT_USER_ID = "user_id"
        private const val ARGUMENT_USER_NAME = "user_name"
        private const val ARGUMENT_CATEGORY = "category"
        private const val STATE_SORT_CRITERIA = "state_sort_criteria"

        fun newInstance(userId: String? = null, userName: String? = null,
                        @CategoryParameter.Category category: String): UserMediaListFragment {
            if (userId.isNullOrBlank() && userName.isNullOrBlank()) {
                throw IllegalArgumentException("You must provide at least one of the arguments")
            }

            return UserMediaListFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_USER_ID, userId)
                    this.putString(ARGUMENT_USER_NAME, userName)
                    this.putString(ARGUMENT_CATEGORY, category)
                }
            }
        }
    }

    override val section = SectionManager.Section.USER_MEDIA_LIST
    override val itemsOnPage = ITEMS_ON_PAGE

    private var userId: String? = null
    private var userName: String? = null

    @CategoryParameter.Category
    private lateinit var category: String

    @UserMediaSortParameter.UserMediaSortCriteria
    private lateinit var sortCriteria: String

    override lateinit var adapter: UserMediaAdapter
    override lateinit var layoutManager: StaggeredGridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ARGUMENT_USER_ID)
        userName = arguments.getString(ARGUMENT_USER_NAME)
        category = arguments.getString(ARGUMENT_CATEGORY)

        if (savedInstanceState != null) {
            sortCriteria = savedInstanceState.getString(STATE_SORT_CRITERIA)
        } else {
            sortCriteria = UserMediaSortParameter.NAME_ASCENDING
        }

        adapter = UserMediaAdapter(savedInstanceState, category)
        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_user_media_list, menu)

        when (sortCriteria) {
            UserMediaSortParameter.NAME_ASCENDING -> {
                menu.findItem(R.id.ascending).isChecked = true
                menu.findItem(R.id.name).isChecked = true
            }

            UserMediaSortParameter.NAME_DESCENDING -> {
                menu.findItem(R.id.descending).isChecked = true
                menu.findItem(R.id.name).isChecked = true
            }

            UserMediaSortParameter.STATE_NAME_ASCENDING -> {
                menu.findItem(R.id.ascending).isChecked = true
                menu.findItem(R.id.state).isChecked = true
            }

            UserMediaSortParameter.STATE_NAME_DESCENDING -> {
                menu.findItem(R.id.descending).isChecked = true
                menu.findItem(R.id.state).isChecked = true
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCriteria = sortCriteria
        when (item.itemId) {
            R.id.ascending -> {
                if (sortCriteria == UserMediaSortParameter.NAME_DESCENDING) {
                    sortCriteria = UserMediaSortParameter.NAME_ASCENDING
                } else if (sortCriteria == UserMediaSortParameter.STATE_NAME_DESCENDING) {
                    sortCriteria = UserMediaSortParameter.STATE_NAME_ASCENDING
                }
            }

            R.id.descending -> {
                if (sortCriteria == UserMediaSortParameter.NAME_ASCENDING) {
                    sortCriteria = UserMediaSortParameter.NAME_DESCENDING
                } else if (sortCriteria == UserMediaSortParameter.STATE_NAME_ASCENDING) {
                    sortCriteria = UserMediaSortParameter.STATE_NAME_DESCENDING
                }
            }

            R.id.name -> {
                if (sortCriteria == UserMediaSortParameter.STATE_NAME_ASCENDING) {
                    sortCriteria = UserMediaSortParameter.NAME_ASCENDING
                } else if (sortCriteria == UserMediaSortParameter.STATE_NAME_DESCENDING) {
                    sortCriteria = UserMediaSortParameter.NAME_DESCENDING
                }
            }

            R.id.state -> {
                if (sortCriteria == UserMediaSortParameter.NAME_ASCENDING) {
                    sortCriteria = UserMediaSortParameter.STATE_NAME_ASCENDING
                } else if (sortCriteria == UserMediaSortParameter.NAME_DESCENDING) {
                    sortCriteria = UserMediaSortParameter.STATE_NAME_DESCENDING
                }
            }

            else -> return false
        }

        if (previousCriteria != sortCriteria) {
            reset()

            item.isChecked = true
        }

        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
        outState.putString(STATE_SORT_CRITERIA, sortCriteria)
    }

    override fun constructPagedLoadingRequest(page: Int):
            LoadingRequest<Array<UserMediaListEntry>> {
        return LoadingRequest(UserMediaListRequest(userId, userName, page).withCategory(category)
                .withSortCriteria(sortCriteria).withLimit(ITEMS_ON_PAGE))
    }
}