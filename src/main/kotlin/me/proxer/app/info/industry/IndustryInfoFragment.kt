package me.proxer.app.info.industry

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import kotterknife.bindView
import linkClicks
import linkLongClicks
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.info.Industry
import me.proxer.library.enums.Country
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class IndustryInfoFragment : BaseContentFragment<Industry>() {

    companion object {
        fun newInstance() = IndustryInfoFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<IndustryInfoViewModel> { parametersOf(id) }

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

        link.linkClicks()
            .map { Utils.getAndFixUrl(it).toOptional() }
            .filterSome()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { showPage(it) }

        link.linkLongClicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                val title = getString(R.string.clipboard_title)

                requireContext().clipboardManager.primaryClip = ClipData.newPlainText(title, it)
                requireContext().toast(R.string.clipboard_status)
            }
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
            link.text = data.link.toString().linkify(mentions = false)
        }

        if (data.description.isBlank()) {
            descriptionContainer.visibility = View.GONE
        } else {
            descriptionContainer.visibility = View.VISIBLE
            description.text = data.description
        }
    }
}
