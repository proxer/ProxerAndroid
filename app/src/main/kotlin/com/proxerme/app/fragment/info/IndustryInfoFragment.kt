package com.proxerme.app.fragment.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.activity.IndustryActivity
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.util.ParameterMapper
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.info.entity.Industry
import com.proxerme.library.connection.info.request.IndustryRequest
import org.jetbrains.anko.toast

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class IndustryInfoFragment : SingleLoadingFragment<String, Industry>() {

    companion object {
        fun newInstance(): IndustryInfoFragment {
            return IndustryInfoFragment()
        }
    }

    override val section = Section.INDUSTRY_INFO

    private val industryActivity
        get() = activity as IndustryActivity

    private val language: ImageView by bindView(R.id.language)
    private val type: TextView by bindView(R.id.type)
    private val link: TextView by bindView(R.id.link)

    private val descriptionContainer: ViewGroup by bindView(R.id.descriptionContainer)
    private val description: TextView by bindView(R.id.description)

    private val id: String
        get() = industryActivity.id
    private var name: String?
        get() = industryActivity.name
        set(value) {
            industryActivity.name = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_industry, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        link.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun present(data: Industry) {
        name = data.name

        language.setImageDrawable(ParameterMapper.country(context, data.country))
        type.text = data.type.replace("_", " ").split(" ").map(String::capitalize)
                .joinToString(separator = " ")
        link.text = Utils.buildClickableText(context, data.link,
                onWebClickListener = Link.OnClickListener { link ->
                    showPage(Utils.parseAndFixUrl(link))
                },
                onWebLongClickListener = Link.OnLongClickListener { link ->
                    Utils.setClipboardContent(activity,
                            getString(R.string.fragment_ucp_overview_clip_title), link)

                    context.toast(R.string.clipboard_status)
                })

        if (data.description.isBlank()) {
            descriptionContainer.visibility = View.GONE
        } else {
            descriptionContainer.visibility = View.VISIBLE
            description.text = data.description
        }
    }

    override fun constructTask() = ProxerLoadingTask(::IndustryRequest)
    override fun constructInput() = id
}