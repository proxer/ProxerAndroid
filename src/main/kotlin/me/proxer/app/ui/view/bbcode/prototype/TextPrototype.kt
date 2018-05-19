package me.proxer.app.ui.view.bbcode.prototype

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.support.v4.util.PatternsCompat
import android.support.v4.widget.TextViewCompat
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.BetterLinkGifAwareEmojiTextView
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.setOnLinkClickListener
import me.proxer.app.util.extension.setOnLinkLongClickListener
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
object TextPrototype : BBPrototype {

    const val TEXT_COLOR_ARGUMENT = "text_color"
    const val TEXT_SIZE_ARGUMENT = "text_size"
    const val TEXT_APPEARANCE_ARGUMENT = "text_appearance"

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = BBArgs(text = code.toSpannableStringBuilder().linkify()))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: BBArgs): List<View> {
        return listOf(makeView(context, args))
    }

    fun makeView(context: Context, args: BBArgs): TextView {
        return applyOnView(BetterLinkGifAwareEmojiTextView(context), args)
    }

    fun applyOnView(view: BetterLinkGifAwareEmojiTextView, args: BBArgs): BetterLinkGifAwareEmojiTextView {
        view.layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT)
        view.text = args.safeText

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }

        applyStyle(args, view)
        setListeners(view)

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
        (args[TEXT_SIZE_ARGUMENT] as? Float)?.let { view.textSize = it }
    }

    private fun setListeners(view: BetterLinkGifAwareEmojiTextView) {
        view.setOnLinkClickListener { textView, link ->
            val baseActivity = BBUtils.findBaseActivity(textView.context)

            when {
                baseActivity == null -> false
                link.startsWith("@") -> {
                    ProfileActivity.navigateTo(baseActivity, null, link.trim().drop(1))

                    true
                }
                PatternsCompat.AUTOLINK_WEB_URL.matcher(link).matches() -> {
                    baseActivity.showPage(Utils.parseAndFixUrl(link))

                    true
                }
                else -> false
            }
        }

        view.setOnLinkLongClickListener { textView, link ->
            if (PatternsCompat.AUTOLINK_WEB_URL.matcher(link).matches()) {
                val title = textView.context.getString(R.string.clipboard_title)

                textView.context.clipboardManager.primaryClip = ClipData.newPlainText(title, link)
                textView.context.toast(R.string.clipboard_status)

                true
            } else {
                false
            }
        }
    }
}
