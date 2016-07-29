package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.adapter.UserMediaAdapter
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.parameters.CategoryParameter.Category
import com.proxerme.library.connection.parameters.SortParameter
import com.proxerme.library.connection.user.request.UserMediaListRequest
import com.proxerme.library.info.ProxerTag

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UserMediaListFragment : PagingFragment() {

    companion object {
        private const val ARGUMENT_USER_ID = "user_id"
        private const val ARGUMENT_USER_NAME = "user_name"
        private const val ARGUMENT_CATEGORY = "category"
        private const val STATE_SORT_CRITERIA = "state_sort_criteria"

        fun newInstance(userId: String? = null, userName: String? = null,
                        @Category category: String): UserMediaListFragment {
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

    override val section: SectionManager.Section = SectionManager.Section.USER_MEDIA_LIST

    private var userId: String? = null
    private var userName: String? = null

    @Category
    private lateinit var category: String

    @SortParameter.SortCriteria
    private lateinit var sortCriteria: String

    private lateinit var adapter: UserMediaAdapter
    override lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ARGUMENT_USER_ID)
        userName = arguments.getString(ARGUMENT_USER_NAME)
        category = arguments.getString(ARGUMENT_CATEGORY)

        if (savedInstanceState != null) {
            sortCriteria = savedInstanceState.getString(STATE_SORT_CRITERIA)
        } else {
            sortCriteria = SortParameter.NAME_ASCENDING
        }

        adapter = UserMediaAdapter(savedInstanceState, category, sortCriteria)
        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_user_media_list, menu)

        when (sortCriteria) {
            SortParameter.NAME_ASCENDING -> {
                menu.findItem(R.id.ascending).isChecked = true
                menu.findItem(R.id.name).isChecked = true
            }

            SortParameter.NAME_DESCENDING -> {
                menu.findItem(R.id.descending).isChecked = true
                menu.findItem(R.id.name).isChecked = true
            }

            SortParameter.STATE_NAME_ASCENDING -> {
                menu.findItem(R.id.ascending).isChecked = true
                menu.findItem(R.id.state).isChecked = true
            }

            SortParameter.STATE_NAME_DESCENDING -> {
                menu.findItem(R.id.descending).isChecked = true
                menu.findItem(R.id.state).isChecked = true
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ascending -> {
                if (sortCriteria == SortParameter.NAME_DESCENDING) {
                    sortCriteria = SortParameter.NAME_ASCENDING

                    reset()
                } else if (sortCriteria == SortParameter.STATE_NAME_DESCENDING) {
                    sortCriteria = SortParameter.STATE_NAME_ASCENDING

                    reset()
                }

                item.isChecked = true

                true
            }

            R.id.descending -> {
                if (sortCriteria == SortParameter.NAME_ASCENDING) {
                    sortCriteria = SortParameter.NAME_DESCENDING

                    reset()
                } else if (sortCriteria == SortParameter.STATE_NAME_ASCENDING) {
                    sortCriteria = SortParameter.STATE_NAME_DESCENDING

                    reset()
                }

                item.isChecked = true

                true
            }

            R.id.name -> {
                if (sortCriteria == SortParameter.STATE_NAME_ASCENDING) {
                    sortCriteria = SortParameter.NAME_ASCENDING

                    reset()
                } else if (sortCriteria == SortParameter.STATE_NAME_DESCENDING) {
                    sortCriteria = SortParameter.NAME_DESCENDING
                }

                item.isChecked = true

                true
            }

            R.id.state -> {
                if (sortCriteria == SortParameter.NAME_ASCENDING) {
                    sortCriteria = SortParameter.STATE_NAME_ASCENDING

                    reset()
                } else if (sortCriteria == SortParameter.NAME_DESCENDING) {
                    sortCriteria = SortParameter.STATE_NAME_DESCENDING
                }

                item.isChecked = true

                true
            }

            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_SORT_CRITERIA, sortCriteria)
        adapter.saveInstanceState(outState)
    }

    override fun loadPage(number: Int) {
        UserMediaListRequest(userId, userName, number)
                .withCategory(category)
                .withLimit(50)
                .withSortCriteria(sortCriteria)
                .execute({ result ->
                    adapter.addItems(result.item.toList())

                    notifyPagedLoadFinishedSuccessful(number, result)
                }, { result ->
                    notifyPagedLoadFinishedWithError(number, result)
                })
    }

    override fun cancel() {
        ProxerConnection.cancel(ProxerTag.USER_MEDIA_LIST)
    }

    override fun clear() {
        adapter.clear()
    }

    override fun reset() {
        adapter.sortCriteria = sortCriteria

        super.reset()
    }
}