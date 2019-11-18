package me.proxer.app.comment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.text.set
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jakewharton.rxbinding3.appcompat.itemClicks
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.ratingChanges
import com.jakewharton.rxbinding3.widget.textChanges
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.util.compat.MenuPopupCompat
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.setIconicsImage
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class EditCommentFragment : BaseContentFragment<LocalComment>(R.layout.fragment_edit_comment) {

    companion object {
        fun newInstance() = EditCommentFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: EditCommentActivity
        get() = activity as EditCommentActivity

    override val viewModel by sharedViewModel<EditCommentViewModel> {
        parametersOf(id, entryId)
    }

    private val scrollContainer by bindView<ViewGroup>(R.id.scrollContainer)

    private val rulesContainer by bindView<ViewGroup>(R.id.rulesContainer)
    private val expandRules by bindView<ImageButton>(R.id.expandRules)
    private val rules by bindView<TextView>(R.id.rules)

    private val ratingTitle by bindView<TextView>(R.id.ratingTitle)
    private val rating by bindView<RatingBar>(R.id.rating)
    private val ratingClear by bindView<ImageButton>(R.id.ratingClear)

    private val editor by bindView<EditText>(R.id.editor)
    private val counter by bindView<TextView>(R.id.counter)

    private val bold by bindView<ImageButton>(R.id.bold)
    private val italic by bindView<ImageButton>(R.id.italic)
    private val underlined by bindView<ImageButton>(R.id.underline)
    private val strikethrough by bindView<ImageButton>(R.id.strikethrough)
    private val size by bindView<ImageButton>(R.id.size)
    private val color by bindView<ImageButton>(R.id.color)
    private val left by bindView<ImageButton>(R.id.left)
    private val center by bindView<ImageButton>(R.id.center)
    private val right by bindView<ImageButton>(R.id.right)
    private val spoiler by bindView<ImageButton>(R.id.spoiler)

    private val commentPreviewBottomSheet by bindView<ViewGroup>(R.id.commentPreviewBottomSheet)
    private val commentPreviewTitle by bindView<TextView>(R.id.commentPreviewTitle)
    private val commentPreview by bindView<BBCodeView>(R.id.commentPreview)
    private val commentPreviewEmpty by bindView<TextView>(R.id.commentPreviewEmpty)

    private val commentBottomSheetBehavior get() = BottomSheetBehavior.from(commentPreviewBottomSheet)

    private val id: String?
        get() = hostingActivity.id

    private val entryId: String?
        get() = hostingActivity.entryId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initListeners()

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_edit_comment, menu, true)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.preview -> commentBottomSheetBehavior.state = when (commentBottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_HIDDEN -> BottomSheetBehavior.STATE_EXPANDED
                else -> BottomSheetBehavior.STATE_HIDDEN
            }
            R.id.publish -> viewModel.publish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showData(data: LocalComment) {
        super.showData(data)

        editor.isFocusableInTouchMode = true
        editor.isFocusable = true

        ratingTitle.text = getString(getRatingTitle(data.overallRating))
        rating.rating = data.overallRating / 2f
        ratingClear.isVisible = data.overallRating > 0f

        if (editor.text.isBlank() && data.content.isNotBlank()) {
            editor.setText(data.content)
        }

        if (data.content.isBlank()) {
            commentPreview.isVisible = false
            commentPreviewEmpty.isVisible = true

            commentPreview.tree = null
        } else {
            commentPreview.isVisible = true
            commentPreviewEmpty.isVisible = false

            commentPreview.tree = data.parsedContent
        }

        if (editor.minHeight == -1) {
            editor.post {
                if (view != null) {
                    editor.minHeight = (counter.y - editor.y).toInt()
                }
            }
        }
    }

    override fun hideData() {
        editor.isFocusableInTouchMode = false
        editor.isFocusable = false

        super.hideData()
    }

    private fun initUI() {
        rules.text = resources.getStringArray(R.array.fragment_edit_comment_rules)
            .joinTo(SpannableStringBuilder(), "\n\n") {
                SpannableString(it.parseAsHtml()).apply { this[0..length] = BulletSpan(requireContext().dip(6)) }
            }

        editor.isNestedScrollingEnabled = false

        expandRules.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)
        ratingClear.setIconicsImage(CommunityMaterial.Icon.cmd_close_circle, 32, paddingDp = 4)
        bold.setIconicsImage(CommunityMaterial.Icon.cmd_format_bold, 32)
        italic.setIconicsImage(CommunityMaterial.Icon.cmd_format_italic, 32)
        underlined.setIconicsImage(CommunityMaterial.Icon.cmd_format_underline, 32)
        strikethrough.setIconicsImage(CommunityMaterial.Icon.cmd_format_strikethrough_variant, 32)
        size.setIconicsImage(CommunityMaterial.Icon.cmd_format_size, 32)
        color.setIconicsImage(CommunityMaterial.Icon.cmd_format_color_fill, 32)
        left.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_left, 32)
        center.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_center, 32)
        right.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_right, 32)
        spoiler.setIconicsImage(CommunityMaterial.Icon.cmd_eye_off, 32)

        commentPreview.expandSpoilers = true
        commentPreviewEmpty.setCompoundDrawables(null, generateEmptyDrawable(), null, null)

        commentBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    @SuppressLint("SetTextI18n")
    private fun initListeners() {
        rulesContainer.clicks().mergeWith(expandRules.clicks())
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                editor.clearFocus()
                rules.isVisible = !rules.isVisible

                TransitionManager.beginDelayedTransition(scrollContainer)

                ViewCompat.animate(expandRules).rotation(if (rules.isVisible) 180f else 0f)
            }

        rating.ratingChanges()
            .skipInitialValue()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.updateRating(it) }

        editor.textChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.updateContent(it.toString()) }

        editor.textChanges()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                editor.requestFocus()
                counter.text = "${it.length} / 20000"
            }

        arrayOf(
            bold to "b", italic to "i", underlined to "u", strikethrough to "s",
            left to "left", center to "center", right to "right", spoiler to "spoiler"
        ).forEach { (button, tag) ->
            button.clicks()
                .autoDisposable(viewLifecycleOwner.scope())
                .subscribe { insertTag(tag) }
        }

        ratingClear.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.updateRating(0f) }

        size.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                PopupMenu(requireContext(), size, Gravity.TOP)
                    .apply {
                        IconicsMenuInflaterUtil.inflate(
                            menuInflater, requireContext(), R.menu.fragment_edit_comment_size, menu
                        )
                    }
                    .apply {
                        itemClicks()
                            .autoDisposable(viewLifecycleOwner.scope())
                            .subscribe {
                                when (it.itemId) {
                                    R.id.tiny -> insertTag("size", "1")
                                    R.id.small -> insertTag("size", "2")
                                    R.id.normal -> insertTag("size", "3")
                                    R.id.large -> insertTag("size", "4")
                                    R.id.huge -> insertTag("size", "5")
                                }
                            }
                    }
                    .let { MenuPopupCompat(requireContext(), it.menu, color) }
                    .show(-dip(48), 0)
            }

        color.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                PopupMenu(requireContext(), color, Gravity.TOP)
                    .apply {
                        IconicsMenuInflaterUtil.inflate(
                            menuInflater, requireContext(), R.menu.fragment_edit_comment_color, menu
                        )
                    }
                    .apply {
                        itemClicks()
                            .autoDisposable(viewLifecycleOwner.scope())
                            .subscribe {
                                when (it.itemId) {
                                    R.id.red -> insertTag("color", getColorString(R.color.md_red_600))
                                    R.id.purple -> insertTag("color", getColorString(R.color.md_purple_600))
                                    R.id.blue -> insertTag("color", getColorString(R.color.md_blue_600))
                                    R.id.green -> insertTag("color", getColorString(R.color.md_green_600))
                                    R.id.yellow -> insertTag("color", getColorString(R.color.md_yellow_600))
                                    R.id.orange -> insertTag("color", getColorString(R.color.md_orange_600))
                                    R.id.grey -> insertTag("color", getColorString(R.color.md_grey_600))
                                    R.id.white -> insertTag("color", getColorString(R.color.md_white_1000))
                                }
                            }
                    }
                    .let { MenuPopupCompat(requireContext(), it.menu, color) }
                    .forceShowIcon()
                    .show(-dip(48), 0)
            }

        commentPreviewTitle.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                editor.clearFocus()
                commentBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
    }

    private fun insertTag(tag: String, value: String = "") {
        val startTag = "[$tag${if (value.isNotEmpty()) "=$value" else ""}]"
        val endTag = "[/$tag]"

        if (editor.selectionStart >= 0 && editor.selectionEnd >= 0) {
            if (editor.selectionStart == editor.selectionEnd) {
                editor.text.insert(editor.selectionStart, "$startTag$endTag")
            } else {
                editor.text.insert(editor.selectionStart, startTag)
                editor.text.insert(editor.selectionEnd, endTag)
            }

            editor.setSelection(editor.selectionEnd - endTag.length)
        } else {
            editor.text.insert(0, "$startTag$endTag")
        }

        editor.requestFocus()
        requireContext().getSystemService<InputMethodManager>()?.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun getRatingTitle(rating: Int) = when (rating) {
        1 -> R.string.fragment_edit_comment_rating_title_1
        2 -> R.string.fragment_edit_comment_rating_title_2
        3 -> R.string.fragment_edit_comment_rating_title_3
        4 -> R.string.fragment_edit_comment_rating_title_4
        5 -> R.string.fragment_edit_comment_rating_title_5
        6 -> R.string.fragment_edit_comment_rating_title_6
        7 -> R.string.fragment_edit_comment_rating_title_7
        8 -> R.string.fragment_edit_comment_rating_title_8
        9 -> R.string.fragment_edit_comment_rating_title_9
        10 -> R.string.fragment_edit_comment_rating_title_10
        else -> R.string.fragment_edit_comment_rating_title_0
    }

    private fun getColorString(@ColorRes color: Int): String {
        return "#${Integer.toHexString(ContextCompat.getColor(requireContext(), color) and 0x00ffffff)}"
    }

    private fun generateEmptyDrawable() = IconicsDrawable(requireContext(), CommunityMaterial.Icon2.cmd_thought_bubble)
        .iconColor(requireContext())
        .sizeDp(128)
}
