package me.proxer.app.info.translatorgroup

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.TranslatorGroup
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
class TranslatorGroupInfoFragment : BaseContentFragment<TranslatorGroup>() {

    companion object {
        fun newInstance() = TranslatorGroupInfoFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { TranslatorGroupInfoViewModelProvider.get(this, id) }

    override val hostingActivity: TranslatorGroupActivity
        get() = activity as TranslatorGroupActivity

    private val id: String
        get() = hostingActivity.id

    private var name: String?
        get() = hostingActivity.name
        set(value) {
            hostingActivity.name = value
        }

    private val language: ImageView by bindView(R.id.language)
    private val linkRow: ViewGroup by bindView(R.id.linkRow)
    private val link: TextView by bindView(R.id.link)
    private val descriptionContainer: ViewGroup by bindView(R.id.descriptionContainer)
    private val description: TextView by bindView(R.id.description)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_translator_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        link.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun showData(data: TranslatorGroup) {
        super.showData(data)

        name = data.name
        language.setImageDrawable(data.country.toAppDrawable(safeContext))

        if (data.link?.toString().isNullOrBlank()) {
            linkRow.visibility = View.GONE
        } else {
            linkRow.visibility = View.VISIBLE
            link.text = Utils.buildClickableText(safeContext, data.link.toString(),
                    onWebClickListener = Link.OnClickListener { link ->
                        showPage(Utils.parseAndFixUrl(link))
                    },
                    onWebLongClickListener = Link.OnLongClickListener { link ->
                        val title = getString(R.string.clipboard_title)

                        safeContext.clipboardManager.primaryClip = ClipData.newPlainText(title, link)
                        safeContext.toast(R.string.clipboard_status)
                    })
        }

        if (data.description.isBlank()) {
            descriptionContainer.visibility = View.GONE
        } else {
            descriptionContainer.visibility = View.VISIBLE
            description.text = data.description
        }
    }
}
