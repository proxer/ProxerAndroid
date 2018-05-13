package me.proxer.app.ui.view.bbcode.prototype

import android.content.ClipData
import android.content.Context
import android.support.v4.widget.TextViewCompat
import android.util.Patterns
import android.view.View
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

    override val startRegex = Regex("x^")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = BBArgs(text = code.toSpannableStringBuilder().linkify()))
    }

    override fun makeViews(context: Context, children: List<BBTree>, args: BBArgs): List<View> {
        return listOf(makeView(context, args.safeText))
    }

    fun makeView(context: Context, text: CharSequence): TextView {
        return applyOnView(BetterLinkGifAwareEmojiTextView(context), text)
    }

    fun applyOnView(view: BetterLinkGifAwareEmojiTextView, args: BBArgs): BetterLinkGifAwareEmojiTextView {
        return applyOnView(view, args.safeText)
    }

    private fun applyOnView(
        view: BetterLinkGifAwareEmojiTextView,
        text: CharSequence
    ): BetterLinkGifAwareEmojiTextView {
        view.text = text

        TextViewCompat.setTextAppearance(view, R.style.TextAppearance_AppCompat_Small)

        view.setOnLinkClickListener { textView, link ->
            val baseActivity = BBUtils.findBaseActivity(textView.context)

            when {
                baseActivity == null -> false
                link.startsWith("@") -> {
                    ProfileActivity.navigateTo(baseActivity, null, link.trim().drop(1))

                    true
                }
                Patterns.WEB_URL.matcher(link).matches() -> {
                    baseActivity.showPage(Utils.parseAndFixUrl(link))

                    true
                }
                else -> false
            }
        }

        view.setOnLinkLongClickListener { textView, link ->
            if (Patterns.WEB_URL.matcher(link).matches()) {
                val title = textView.context.getString(R.string.clipboard_title)

                textView.context.clipboardManager.primaryClip = ClipData.newPlainText(title, link)
                textView.context.toast(R.string.clipboard_status)

                true
            } else {
                false
            }
        }

        return view
    }
}
