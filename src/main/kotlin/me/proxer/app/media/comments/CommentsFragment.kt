package me.proxer.app.media.comments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.comment.EditCommentActivity
import me.proxer.app.comment.LocalComment
import me.proxer.app.media.MediaActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.getSafeParcelableExtra
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import me.proxer.library.enums.CommentSortCriteria
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class CommentsFragment : PagedContentFragment<ParsedComment>(R.layout.fragment_comments) {

    companion object {
        private const val SORT_CRITERIA_ARGUMENT = "sort_criteria"

        fun newInstance() = CommentsFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_comments
    override val isSwipeToRefreshEnabled = true
    override val pagingThreshold = 3

    override val viewModel by viewModel<CommentsViewModel> { parametersOf(id, sortCriteria) }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = hostingActivity.id

    private val category: Category?
        get() = hostingActivity.category

    private val name: String?
        get() = hostingActivity.name

    private var sortCriteria: CommentSortCriteria
        get() = requireArguments().getSerializable(SORT_CRITERIA_ARGUMENT) as? CommentSortCriteria
            ?: CommentSortCriteria.RATING
        set(value) {
            requireArguments().putSerializable(SORT_CRITERIA_ARGUMENT, value)

            viewModel.sortCriteria = value
        }

    override val layoutManager by unsafeLazy { LinearLayoutManager(context) }

    override var innerAdapter by Delegates.notNull<CommentsAdapter>()

    private val create by bindView<FloatingActionButton>(R.id.create)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = CommentsAdapter(savedInstanceState, storageHelper)

        innerAdapter.profileClickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, comment) ->
                ProfileActivity.navigateTo(
                    requireActivity(), comment.authorId, comment.author, comment.image,
                    if (view.drawable != null && comment.image.isNotBlank()) view else null
                )
            }

        innerAdapter.editClickSubject
            .autoDisposable(this.scope())
            .subscribe {
                EditCommentActivity.navigateTo(this, it.id, it.entryId, name)
            }

        innerAdapter.deleteClickSubject
            .autoDisposable(this.scope())
            .subscribe { comment ->
                MaterialDialog(requireContext())
                    .message(text = getString(R.string.dialog_comment_delete_message, comment.author).parseAsHtml())
                    .negativeButton(res = R.string.cancel)
                    .positiveButton(res = R.string.dialog_comment_delete_positive) {
                        viewModel.deleteComment(comment)
                    }
                    .show()
            }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
        innerAdapter.categoryCallback = { category }

        hostingActivity.headerHeightChanges()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { create.translationY = it }

        viewModel.itemDeletionError.observe(viewLifecycleOwner, Observer {
            it?.let {
                hostingActivity.multilineSnackbar(
                    getString(R.string.error_comment_deletion, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                )
            }
        })

        create.setImageDrawable(
            IconicsDrawable(requireContext(), CommunityMaterial.Icon3.cmd_pencil).apply {
                colorInt = requireContext().resolveColor(R.attr.colorOnPrimary)
                paddingDp = 8
                sizeDp = 64
            }
        )

        create.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { EditCommentActivity.navigateTo(this, entryId = id, name = name) }

        TooltipCompat.setTooltipText(create, getString(R.string.action_write_comment))
    }

    override fun onDestroy() {
        innerAdapter.categoryCallback = null

        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EditCommentActivity.COMMENT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Single.fromCallable { data.getSafeParcelableExtra<LocalComment>(EditCommentActivity.COMMENT_EXTRA) }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(this.scope())
                .subscribeAndLogErrors { viewModel.updateComment(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_comments, menu, true)

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
