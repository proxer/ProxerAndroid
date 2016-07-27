package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
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
    private lateinit var category: String

    private lateinit var adapter: UserMediaAdapter
    override lateinit var layoutManager: GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ARGUMENT_USER_ID)
        userName = arguments.getString(ARGUMENT_USER_NAME)
        category = arguments.getString(ARGUMENT_CATEGORY)

        adapter = UserMediaAdapter(savedInstanceState, category)
        layoutManager = GridLayoutManager(context, Utils.calculateSpanAmount(activity) + 1)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun loadPage(number: Int) {
        UserMediaListRequest(userId, userName, number)
                .withCategory(category)
                .withLimit(25)
                .withSortCriteria(SortParameter.NAME_ASCENDING)
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
}