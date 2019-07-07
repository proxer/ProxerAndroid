package me.proxer.app.comment

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.text.set
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.jakewharton.rxbinding3.appcompat.itemClicks
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.focusChanges
import com.jakewharton.rxbinding3.widget.ratingChanges
import com.jakewharton.rxbinding3.widget.textChanges
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class CommentEditFragment : BaseContentFragment<LocalComment>(R.layout.fragment_comment_edit) {

    companion object {
        fun newInstance() = CommentEditFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: CommentActivity
        get() = activity as CommentActivity

    override val viewModel by sharedViewModel<CommentViewModel> {
        parametersOf(id, entryId)
    }

    private val rulesContainer by bindView<ViewGroup>(R.id.rulesContainer)
    private val expandRules by bindView<ImageButton>(R.id.expandRules)
    private val rules by bindView<TextView>(R.id.rules)
    private val ratingTitle by bindView<TextView>(R.id.ratingTitle)
    private val rating by bindView<RatingBar>(R.id.rating)
    private val ratingClear by bindView<ImageButton>(R.id.ratingClear)
    private val editor by bindView<EditText>(R.id.editor)
    private val formatterBar by bindView<ViewGroup>(R.id.formatterBar)
    private val bold by bindView<ImageButton>(R.id.bold)
    private val italic by bindView<ImageButton>(R.id.italic)
    private val underlined by bindView<ImageButton>(R.id.underline)
    private val strikethrough by bindView<ImageButton>(R.id.strikethrough)
    private val size by bindView<ImageButton>(R.id.size)
    private val left by bindView<ImageButton>(R.id.left)
    private val center by bindView<ImageButton>(R.id.center)
    private val right by bindView<ImageButton>(R.id.right)
    private val spoiler by bindView<ImageButton>(R.id.spoiler)

    private val inputMethodManager by unsafeLazy {
        requireNotNull(requireContext().getSystemService<InputMethodManager>())
    }

    private val id: String?
        get() = hostingActivity.id

    private val entryId: String?
        get() = hostingActivity.entryId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rules.text = resources.getStringArray(R.array.fragment_comment_rules)
            .joinTo(SpannableStringBuilder(), "\n\n") {
                SpannableString(it.parseAsHtml()).apply { this[0..length] = BulletSpan(requireContext().dip(6)) }
            }

        expandRules.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)
        ratingClear.setIconicsImage(CommunityMaterial.Icon.cmd_close_circle, 32, paddingDp = 4)
        bold.setIconicsImage(CommunityMaterial.Icon.cmd_format_bold, 32)
        italic.setIconicsImage(CommunityMaterial.Icon.cmd_format_italic, 32)
        underlined.setIconicsImage(CommunityMaterial.Icon.cmd_format_underline, 32)
        strikethrough.setIconicsImage(CommunityMaterial.Icon.cmd_format_strikethrough_variant, 32)
        size.setIconicsImage(CommunityMaterial.Icon.cmd_format_size, 32)
        left.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_left, 32)
        center.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_center, 32)
        right.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_right, 32)
        spoiler.setIconicsImage(CommunityMaterial.Icon.cmd_eye_off, 32)

        rulesContainer.clicks().mergeWith(expandRules.clicks())
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                rules.isVisible = !rules.isVisible

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

        editor.focusChanges()
            .skipInitialValue()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { if (it) viewModel.hasFocused = true }

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
                            menuInflater, requireContext(), R.menu.fragment_comment_size, menu
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
                    .show()
            }
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.data.value != null && viewModel.hasFocused) {
            focusEditor()
        }
    }

    override fun onPause() {
        clearEditorFocus()

        super.onPause()
    }

    override fun showData(data: LocalComment) {
        super.showData(data)

        formatterBar.isVisible = true

        ratingTitle.text = getString(getRatingTitle(data.overallRating))
        rating.rating = data.overallRating / 2f
        ratingClear.isVisible = data.overallRating > 0f

        if (editor.text.isBlank()) {
            if (data.content.isNotBlank()) {
                editor.setText(data.content)
            } else if (!viewModel.hasFocused) {
                focusEditor()
            }
        }
    }

    override fun hideData() {
        editor.text = null
        formatterBar.isVisible = false

        super.hideData()
    }

    private fun insertTag(tag: String, value: String = "") {
        val startTag = "[$tag${if (value.isNotEmpty()) "=$value" else ""}]"
        val endTag = "[/$tag]"

        if (editor.selectionStart >= 0) {
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

        focusEditor()
    }

    private fun getRatingTitle(rating: Int) = when (rating) {
        1 -> R.string.fragment_comment_rating_title_1
        2 -> R.string.fragment_comment_rating_title_2
        3 -> R.string.fragment_comment_rating_title_3
        4 -> R.string.fragment_comment_rating_title_4
        5 -> R.string.fragment_comment_rating_title_5
        6 -> R.string.fragment_comment_rating_title_6
        7 -> R.string.fragment_comment_rating_title_7
        8 -> R.string.fragment_comment_rating_title_8
        9 -> R.string.fragment_comment_rating_title_9
        10 -> R.string.fragment_comment_rating_title_10
        else -> R.string.fragment_comment_rating_title_0
    }

    private fun focusEditor() {
        viewModel.hasFocused = true

        editor.requestFocus()
        inputMethodManager.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun clearEditorFocus() {
        editor.clearFocus()
        inputMethodManager.hideSoftInputFromWindow(editor.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}
