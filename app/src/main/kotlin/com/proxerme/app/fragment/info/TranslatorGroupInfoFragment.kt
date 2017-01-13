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
import com.proxerme.app.activity.TranslatorGroupActivity
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.info.entity.TranslatorGroup
import com.proxerme.library.connection.info.request.TranslatorGroupRequest
import com.proxerme.library.parameters.CountryParameter
import org.jetbrains.anko.toast

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class TranslatorGroupInfoFragment : SingleLoadingFragment<String, TranslatorGroup>() {

    companion object {
        private const val ARGUMENT_ID = "id"

        fun newInstance(id: String): TranslatorGroupInfoFragment {
            return TranslatorGroupInfoFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.TRANSLATOR_GROUP_INFO

    private val language: ImageView by bindView(R.id.language)
    private val link: TextView by bindView(R.id.link)

    private val descriptionContainer: ViewGroup by bindView(R.id.descriptionContainer)
    private val description: TextView by bindView(R.id.description)

    private val id: String
        get() = arguments.getString(ARGUMENT_ID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_translator_group, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        link.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun present(data: TranslatorGroup) {
        (activity as TranslatorGroupActivity).updateName(data.name)

        // TODO: Better icons
        language.setImageResource(when (data.country) {
            CountryParameter.ENGLISH -> R.drawable.ic_united_states
            CountryParameter.GERMAN -> R.drawable.ic_germany
            CountryParameter.JAPANESE -> R.drawable.ic_japan
            CountryParameter.MISCELLANEOUS -> R.drawable.ic_proxer
            "us" -> R.drawable.ic_united_states
            else -> throw IllegalArgumentException("Unknown country: ${data.country}")
        })

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

    override fun constructTask() = ProxerLoadingTask(::TranslatorGroupRequest)
    override fun constructInput() = id
}