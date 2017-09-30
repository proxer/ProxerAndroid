package me.proxer.app.media.comment

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.AndroidLifecycle
import com.uber.autodispose.kotlin.autoDisposeWith
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.Comment
import me.proxer.library.enums.Category
import me.proxer.library.enums.CommentSortCriteria
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class CommentFragment : PagedContentFragment<Comment>() {

    companion object {
        private const val SORT_CRITERIA_ARGUMENT = "sort_criteria"

        fun newInstance() = CommentFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_comments
    override val isSwipeToRefreshEnabled = true
    override val pagingThreshold = 3

    override val viewModel by unsafeLazy { CommentViewModelProvider.get(this, id, sortCriteria) }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = hostingActivity.id

    private val category: Category?
        get() = hostingActivity.category

    private var sortCriteria: CommentSortCriteria
        get() = arguments.getSerializable(SORT_CRITERIA_ARGUMENT) as? CommentSortCriteria ?: CommentSortCriteria.RATING
        set(value) {
            arguments.putSerializable(SORT_CRITERIA_ARGUMENT, value)

            viewModel.sortCriteria = value
        }

    override val layoutManager by unsafeLazy { LinearLayoutManager(context) }

    override var innerAdapter by Delegates.notNull<CommentAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = CommentAdapter(savedInstanceState)

        innerAdapter.profileClickSubject
                .autoDisposeWith(AndroidLifecycle.from(this))
                .subscribe { (view, comment) ->
                    ProfileActivity.navigateTo(activity, comment.authorId, comment.author, comment.image,
                            if (view.drawable != null && comment.image.isNotBlank()) view else null)
                }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
        innerAdapter.categoryCallback = { category }
    }

    override fun onDestroy() {
        innerAdapter.categoryCallback = null

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_comments, menu, true)

        when (sortCriteria) {
            CommentSortCriteria.RATING -> menu.findItem(R.id.rating).isChecked = true
            CommentSortCriteria.TIME -> menu.findItem(R.id.time).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.rating -> sortCriteria = CommentSortCriteria.RATING
            R.id.time -> sortCriteria = CommentSortCriteria.TIME
        }

        item.isChecked = true

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }
}
