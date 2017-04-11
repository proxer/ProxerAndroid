package me.proxer.app.fragment.info

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
import me.proxer.app.activity.TranslatorGroupActivity
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.TranslatorGroup
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
class TranslatorGroupInfoFragment : LoadingFragment<ProxerCall<TranslatorGroup>, TranslatorGroup>() {

    companion object {
        fun newInstance(): TranslatorGroupInfoFragment {
            return TranslatorGroupInfoFragment()
        }
    }

    private val translatorGroupActivity
        get() = activity as TranslatorGroupActivity

    private val id: String
        get() = translatorGroupActivity.id

    private var name: String?
        get() = translatorGroupActivity.name
        set(value) {
            translatorGroupActivity.name = value
        }

    private val language: ImageView by bindView(R.id.language)
    private val linkRow: ViewGroup by bindView(R.id.linkRow)
    private val link: TextView by bindView(R.id.link)
    private val descriptionContainer: ViewGroup by bindView(R.id.descriptionContainer)
    private val description: TextView by bindView(R.id.description)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_translator_group, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        link.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun onSuccess(result: TranslatorGroup) {
        name = result.name

        language.setImageDrawable(result.country.toAppDrawable(context))

        if (result.link?.toString().isNullOrBlank()) {
            linkRow.visibility = View.GONE
        } else {
            linkRow.visibility = View.VISIBLE
            link.text = Utils.buildClickableText(context, result.link.toString(),
                    onWebClickListener = Link.OnClickListener { link ->
                        showPage(Utils.parseAndFixUrl(link))
                    },
                    onWebLongClickListener = Link.OnLongClickListener { link ->
                        Utils.setClipboardContent(activity, getString(R.string.clipboard_title), link)

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

    override fun constructTask() = TaskBuilder.asyncProxerTask<TranslatorGroup>().build()
    override fun constructInput() = api.info().translatorGroup(id).build()
}