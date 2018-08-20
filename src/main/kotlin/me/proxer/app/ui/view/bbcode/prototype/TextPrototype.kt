package me.proxer.app.ui.view.bbcode.prototype

import android.content.ClipData
import android.os.Build
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.util.PatternsCompat
import androidx.core.widget.TextViewCompat
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.functions.Predicate
import linkClicks
import linkLongClicks
import me.proxer.app.R
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.linkify
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
object TextPrototype : BBPrototype {

    const val TEXT_COLOR_ARGUMENT = "text_color"
    const val TEXT_SIZE_ARGUMENT = "text_size"
    const val TEXT_APPEARANCE_ARGUMENT = "text_appearance"

    private val WEB_URL_REGEX = PatternsCompat.AUTOLINK_WEB_URL.toRegex()
    private val VALID_LINK_PREDICATE = Predicate<String> { it.startsWith("@") || WEB_URL_REGEX.matches(it) }

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = BBArgs(text = code.toSpannableStringBuilder().linkify()))
    }

    override fun makeViews(parent: BBCodeView, children: List<BBTree>, args: BBArgs): List<View> {
        return listOf(makeView(parent, args))
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
        view.text = args.safeText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
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
        view.linkClicks(VALID_LINK_PREDICATE)
            .autoDisposable(ViewScopeProvider.from(parent))
            .subscribe {
                val baseActivity = BBUtils.findBaseActivity(parent.context) ?: return@subscribe

                when {
                    it.startsWith("@") -> ProfileActivity.navigateTo(baseActivity, null, it.trim().drop(1))
                    WEB_URL_REGEX.matches(it) -> baseActivity.showPage(Utils.getAndFixUrl(it))
                }
            }

        view.linkLongClicks(Predicate { WEB_URL_REGEX.matches(it) })
            .autoDisposable(ViewScopeProvider.from(parent))
            .subscribe {
                val title = view.context.getString(R.string.clipboard_title)

                parent.context.clipboardManager.primaryClip = ClipData.newPlainText(title, it)
                parent.context.toast(R.string.clipboard_status)
            }
    }
}
