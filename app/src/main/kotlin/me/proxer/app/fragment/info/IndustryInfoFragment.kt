package me.proxer.app.fragment.info

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.IndustryActivity
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.Industry
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
class IndustryInfoFragment : LoadingFragment<ProxerCall<Industry>, Industry>() {

    companion object {
        fun newInstance(): IndustryInfoFragment {
            return IndustryInfoFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    private val industryActivity
        get() = activity as IndustryActivity

    private val id: String
        get() = industryActivity.id

    private var name: String?
        get() = industryActivity.name
        set(value) {
            industryActivity.name = value
        }

    private val language: ImageView by bindView(R.id.language)
    private val type: TextView by bindView(R.id.type)
    private val linkRow: ViewGroup by bindView(R.id.linkRow)
    private val link: TextView by bindView(R.id.link)
    private val descriptionContainer: ViewGroup by bindView(R.id.descriptionContainer)
    private val description: TextView by bindView(R.id.description)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_industry, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        link.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun onSuccess(result: Industry) {
        name = result.name
        language.setImageDrawable(result.country.toAppDrawable(context))
        type.text = ProxerUtils.getApiEnumName(result.type)
                ?.replace("_", " ")
                ?.split(" ")
                ?.joinToString(separator = " ", transform = String::capitalize) ?: throw NullPointerException()

        if (result.link?.toString().isNullOrBlank()) {
            linkRow.visibility = View.GONE
        } else {
            linkRow.visibility = View.VISIBLE
            link.text = Utils.buildClickableText(context, result.link.toString(),
                    onWebClickListener = Link.OnClickListener { link ->
                        showPage(Utils.parseAndFixUrl(link))
                    },
                    onWebLongClickListener = Link.OnLongClickListener { link ->
                        val title = getString(R.string.clipboard_title)

                        context.clipboardManager.primaryClip = ClipData.newPlainText(title, link)
                        context.toast(R.string.clipboard_status)
                    })
        }

        if (result.description.isBlank()) {
            descriptionContainer.visibility = View.GONE
        } else {
            descriptionContainer.visibility = View.VISIBLE
            description.text = result.description
        }

        super.onSuccess(result)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<Industry>().build()
    override fun constructInput() = api.info().industry(id).build()
}