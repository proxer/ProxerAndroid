package me.proxer.app.fragment.profile

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.mikepenz.iconics.utils.IconicsMenuInflatorUtil
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.adapter.profile.UserCommentAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.user.UserComment
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class UserCommentsFragment : PagedLoadingFragment<ProxerCall<List<UserComment>>, UserComment>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"

        fun newInstance(): UserCommentsFragment {
            return UserCommentsFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val emptyResultMessage = R.string.error_no_data_comments
    override val isSwipeToRefreshEnabled = false
    override val pagingThreshold = 3
    override val itemsOnPage = 10

    private val profileActivity
        get() = activity as ProfileActivity

    private val userId: String?
        get() = profileActivity.userId

    private val username: String?
        get() = profileActivity.username

    private var category: Category?
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as? Category
        set(value) = arguments.putSerializable(CATEGORY_ARGUMENT, value)

    override val layoutManager by lazy { LinearLayoutManager(context) }
    override lateinit var innerAdapter: UserCommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = UserCommentAdapter(savedInstanceState)
        innerAdapter.callback = object : UserCommentAdapter.UserCommentAdapterCallback {
            override fun onTitleClick(item: UserComment) {
                MediaActivity.navigateTo(activity, item.entryId, item.entryName, item.category)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflatorUtil.inflate(inflater, context, R.menu.fragment_user_comments, menu, true)

        when (category) {
            Category.ANIME -> menu.findItem(R.id.anime).isChecked = true
            Category.MANGA -> menu.findItem(R.id.manga).isChecked = true
            else -> menu.findItem(R.id.all).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCategory = category

        when (item.itemId) {
            R.id.anime -> category = Category.ANIME
            R.id.manga -> category = Category.MANGA
            R.id.all -> category = null
        }

        if (category != previousCategory) {
            item.isChecked = true

            freshLoad()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<UserComment>>().build()
    override fun constructPagedInput(page: Int) = api.user().comments(userId, username)
            .page(page)
            .limit(itemsOnPage)
            .category(category)
            .build()
}
