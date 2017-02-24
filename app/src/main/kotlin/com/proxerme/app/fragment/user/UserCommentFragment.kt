package com.proxerme.app.fragment.user

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.proxerme.app.R
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.adapter.media.CommentAdapter
import com.proxerme.app.fragment.framework.PagedLoadingFragment
import com.proxerme.app.fragment.user.UserCommentFragment.UserCommentInput
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.connection.user.request.UserCommentRequest
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UserCommentFragment : PagedLoadingFragment<UserCommentInput, Comment>() {

    companion object {
        fun newInstance(): UserCommentFragment {
            return UserCommentFragment()
        }
    }

    override val section = Section.COMMENTS
    override val itemsOnPage = 25

    private val profileActivity
        get() = activity as ProfileActivity

    private var category = CategoryParameter.ANIME

    private val userId: String?
        get() = profileActivity.userId
    private val username: String?
        get() = profileActivity.username

    override lateinit var adapter: CommentAdapter
    override lateinit var layoutManager: LinearLayoutManager

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
        inflater.inflate(R.menu.fragment_user_comments, menu)

        when (category) {
            CategoryParameter.ANIME -> menu.findItem(R.id.anime).isChecked = true
            CategoryParameter.MANGA -> menu.findItem(R.id.manga).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCategory = category

        when (item.itemId) {
            R.id.anime -> category = CategoryParameter.ANIME
            R.id.manga -> category = CategoryParameter.MANGA
            else -> return false
        }

        if (category != previousCategory) {
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

    override fun constructTask(): Task<UserCommentInput, Array<Comment>> {
        return ProxerLoadingTask({
            UserCommentRequest(it.userId, it.username)
                    .withPage(it.page)
                    .withLimit(it.itemsOnPage)
                    .withCategory(it.category)
        })
    }

    override fun constructInput(page: Int): UserCommentInput {
        return UserCommentInput(page, userId, username, itemsOnPage, category)
    }

    override fun getEmptyMessage(): Int {
        return R.string.error_no_data_comments
    }

    class UserCommentInput(page: Int, val userId: String?, val username: String?,
                           val itemsOnPage: Int, val category: String) : PagedInput(page)
}
