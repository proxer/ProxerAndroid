package me.proxer.app.ui.view.bbcode.prototype

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.text.Spannable
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.util.PatternsCompat
import androidx.core.widget.TextViewCompat
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.extension.linkClicks
import me.proxer.app.util.extension.linkLongClicks
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.toPrefixedHttpUrl
import me.proxer.app.util.extension.toast

/**
 * @author Ruben Gees
 */
object TextPrototype : BBPrototype, ContentPrototype {

    const val TEXT_COLOR_ARGUMENT = "text_color"
    const val TEXT_SIZE_ARGUMENT = "text_size"
    const val TEXT_APPEARANCE_ARGUMENT = "text_appearance"

    private val spannableFactory = object : Spannable.Factory() {
        override fun newSpannable(source: CharSequence): Spannable {
            return source as Spannable
        }
    }

    @SuppressLint("RestrictedApi")
    private val webUrlRegex = PatternsCompat.AUTOLINK_WEB_URL.toRegex()
    private val validLinkPredicate = { link: String -> link.startsWith("@") || webUrlRegex.matches(link) }

    @Suppress("RegExpUnexpectedAnchor")
    override val startRegex = Regex("x^")

    @Suppress("RegExpUnexpectedAnchor")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = BBArgs(text = code.toSpannableStringBuilder().linkify()))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        return listOf(makeView(parent, args))
    }

    override fun isBlank(args: BBArgs): Boolean {
        return args.text?.isBlank() != false
    }

    fun makeView(parent: BBCodeView, args: BBArgs): TextView {
        return applyOnView(parent, BetterLinkGifAwareEmojiTextView(parent.context), args)
    }

    fun applyOnView(
        parent: BBCodeView,
        view: BetterLinkGifAwareEmojiTextView,
        args: BBArgs
    ): BetterLinkGifAwareEmojiTextView {
        view.layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)

        view.setSpannableFactory(spannableFactory)
        view.setText(args.safeText, TextView.BufferType.SPANNABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        applyStyle(args, view)
        setListeners(parent, view)

        return view
    }

    private fun applyStyle(args: BBArgs, view: BetterLinkGifAwareEmojiTextView) {
        (args[TEXT_APPEARANCE_ARGUMENT] as? Int).let {
            if (it == null) {
                TextViewCompat.setTextAppearance(view, R.style.TextAppearance_AppCompat_Small)
            } else {
                TextViewCompat.setTextAppearance(view, it)
            }
        }

        (args[TEXT_COLOR_ARGUMENT] as? Int)?.let { view.setTextColor(it) }
        (args[TEXT_SIZE_ARGUMENT] as? Float)?.let { view.setTextSize(COMPLEX_UNIT_PX, it) }
    }

    private fun setListeners(parent: BBCodeView, view: BetterLinkGifAwareEmojiTextView) {
        view.linkClicks(validLinkPredicate)
            .autoDisposable(ViewScopeProvider.from(parent))
            .subscribe {
                val baseActivity = BBUtils.findBaseActivity(parent.context) ?: return@subscribe

                when {
                    it.startsWith("@") -> ProfileActivity.navigateTo(baseActivity, null, it.trim().drop(1))
                    webUrlRegex.matches(it) -> baseActivity.showPage(it.toPrefixedHttpUrl())
                }
            }

        view.linkLongClicks { webUrlRegex.matches(it) }
            .autoDisposable(ViewScopeProvider.from(parent))
            .subscribe {
                val title = view.context.getString(R.string.clipboard_title)

                parent.context.getSystemService<ClipboardManager>()?.setPrimaryClip(
                    ClipData.newPlainText(title, it.toString())
                )

                parent.context.toast(R.string.clipboard_status)
            }
    }
}
