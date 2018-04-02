package me.proxer.app.info.industry

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.klinker.android.link_builder.TouchableMovementMethod
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.Industry
import me.proxer.library.enums.Country
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
class IndustryInfoFragment : BaseContentFragment<Industry>() {

    companion object {
        fun newInstance() = IndustryInfoFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { IndustryInfoViewModelProvider.get(this, id) }

    override val hostingActivity: IndustryActivity
        get() = activity as IndustryActivity

    private val id: String
        get() = hostingActivity.id

    private var name: String?
        get() = hostingActivity.name
        set(value) {
            hostingActivity.name = value
        }

    private val languageRow: ViewGroup by bindView(R.id.languageRow)
    private val language: ImageView by bindView(R.id.language)
    private val type: TextView by bindView(R.id.type)
    private val linkRow: ViewGroup by bindView(R.id.linkRow)
    private val link: TextView by bindView(R.id.link)
    private val descriptionContainer: ViewGroup by bindView(R.id.descriptionContainer)
    private val description: TextView by bindView(R.id.description)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_industry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        link.movementMethod = TouchableMovementMethod.instance
    }

    override fun showData(data: Industry) {
        super.showData(data)

        name = data.name
        type.text = data.type.toAppString(requireContext())

        if (data.country == Country.NONE) {
            languageRow.visibility = View.GONE
        } else {
            languageRow.visibility = View.VISIBLE
            language.setImageDrawable(data.country.toAppDrawable(requireContext()))
        }

        if (data.link?.toString().isNullOrBlank()) {
            linkRow.visibility = View.GONE
        } else {
            linkRow.visibility = View.VISIBLE
            link.text = Utils.buildClickableText(requireContext(), data.link.toString(),
                onWebClickListener = { link -> showPage(Utils.parseAndFixUrl(link)) },
                onWebLongClickListener = { link ->
                    val title = getString(R.string.clipboard_title)

                    requireContext().clipboardManager.primaryClip = ClipData.newPlainText(title, link)
                    requireContext().toast(R.string.clipboard_status)
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
