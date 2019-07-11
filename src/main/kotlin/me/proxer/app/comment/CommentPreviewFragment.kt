package me.proxer.app.comment

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.util.extension.iconColor
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class CommentPreviewFragment : BaseContentFragment<LocalComment>(R.layout.fragment_comment_preview) {

    companion object {
        fun newInstance() = CommentPreviewFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: CommentActivity
        get() = activity as CommentActivity

    override val viewModel by sharedViewModel<CommentViewModel> {
        parametersOf(id, entryId)
    }

    private val commentContainer by bindView<ViewGroup>(R.id.commentContainer)
    private val comment by bindView<BBCodeView>(R.id.comment)
    private val commentEmpty by bindView<TextView>(R.id.commentEmpty)

    private val id: String?
        get() = hostingActivity.id

    private val entryId: String?
        get() = hostingActivity.entryId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        comment.expandSpoilers = true

        commentEmpty.setCompoundDrawables(null, generateEmptyDrawable(), null, null)
    }

    override fun showData(data: LocalComment) {
        super.showData(data)

        if (data.content.isBlank()) {
            commentContainer.isVisible = false
            commentEmpty.isVisible = true

            comment.tree = null
        } else {
            commentContainer.isVisible = true
            commentEmpty.isVisible = false

            comment.tree = data.parsedContent
        }
    }

    override fun hideData() {
        comment.tree = null

        super.hideData()
    }

    private fun generateEmptyDrawable() = IconicsDrawable(requireContext(), CommunityMaterial.Icon2.cmd_thought_bubble)
        .iconColor(requireContext())
        .sizeDp(128)
}
