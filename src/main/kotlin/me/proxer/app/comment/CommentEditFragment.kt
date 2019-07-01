package me.proxer.app.comment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.jakewharton.rxbinding3.appcompat.itemClicks
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
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

    private val inputMethodManager by unsafeLazy {
        requireNotNull(requireContext().getSystemService<InputMethodManager>())
    }

    private val id: String?
        get() = hostingActivity.id

    private val entryId: String?
        get() = hostingActivity.entryId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editor.textChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.updateContent(it.toString()) }

        bold.setIconicsImage(CommunityMaterial.Icon.cmd_format_bold, 32)
        italic.setIconicsImage(CommunityMaterial.Icon.cmd_format_italic, 32)
        underlined.setIconicsImage(CommunityMaterial.Icon.cmd_format_underline, 32)
        strikethrough.setIconicsImage(CommunityMaterial.Icon.cmd_format_strikethrough_variant, 32)
        size.setIconicsImage(CommunityMaterial.Icon.cmd_format_size, 32)
        left.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_left, 32)
        right.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_center, 32)
        center.setIconicsImage(CommunityMaterial.Icon.cmd_format_align_right, 32)

        arrayOf(
            bold to "b", italic to "i", underlined to "u", strikethrough to "s",
            left to "left", center to "center", right to "right"
        ).forEach { (button, tag) ->
            button.clicks()
                .autoDisposable(viewLifecycleOwner.scope())
                .subscribe { insertTag(tag) }
        }

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

        if (viewModel.data.value != null) {
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

        if (editor.text.isBlank()) {
            if (data.content.isNotBlank()) {
                editor.setText(data.content)
            }

            focusEditor()
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

    private fun focusEditor() {
        editor.requestFocus()
        inputMethodManager.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun clearEditorFocus() {
        editor.clearFocus()
        inputMethodManager.hideSoftInputFromWindow(editor.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }
}
