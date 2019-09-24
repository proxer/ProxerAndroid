package me.proxer.app.profile.about

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.ProxerWebView
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.linkClicks
import me.proxer.app.util.extension.linkLongClicks
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.extension.toast
import me.proxer.library.entity.user.UserAbout
import me.proxer.library.enums.Gender
import me.proxer.library.enums.RelationshipStatus
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.math.BigDecimal
import java.math.MathContext

/**
 * @author Ruben Gees
 */
class ProfileAboutFragment : BaseContentFragment<UserAbout>(R.layout.fragment_about) {

    companion object {
        private const val ZERO_DATE = "0000-00-00"

        fun newInstance() = ProfileAboutFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: ProfileActivity
        get() = activity as ProfileActivity

    override val viewModel by viewModel<ProfileAboutViewModel> {
        parametersOf(userId, username)
    }

    private val userId: String?
        get() = hostingActivity.userId

    private val username: String?
        get() = hostingActivity.username

    private val generalContainer by bindView<ViewGroup>(R.id.generalContainer)
    private val generalTable by bindView<TableLayout>(R.id.generalTable)
    private val aboutContainer by bindView<ViewGroup>(R.id.aboutContainer)
    private val about by bindView<ProxerWebView>(R.id.about)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        about.showPageSubject
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { showPage(it) }
    }

    override fun onDestroyView() {
        about.destroy()

        super.onDestroyView()
    }

    override fun showData(data: UserAbout) {
        super.showData(data)

        addTableRows(data)

        data.about.let {
            when (it.isNotBlank()) {
                true -> about.loadDataWithBaseURL(
                    null,
                    constructHtmlSkeleton(it),
                    "text/html; charset=utf-8",
                    "utf-8",
                    null
                )
                false -> aboutContainer.isGone = true
            }
        }

        if (generalTable.childCount <= 0) {
            generalContainer.isGone = true
        }

        if (generalContainer.isGone && aboutContainer.isGone) {
            showError(ErrorAction(R.string.error_no_data_profile_about, ACTION_MESSAGE_HIDE))
        }
    }

    private fun addTableRows(data: UserAbout) {
        val normalizedGender = when (data.gender) {
            Gender.UNKNOWN -> ""
            else -> data.gender.toAppString(requireContext())
        }

        val normalizedRelationshipStatus = when (data.relationshipStatus) {
            RelationshipStatus.UNKNOWN -> ""
            else -> data.relationshipStatus.toAppString(requireContext())
        }

        val normalizedBirthday = when (data.birthday) {
            ZERO_DATE -> ""
            else -> data.birthday.split("-").let {
                when (it.size) {
                    3 -> {
                        val (year, month, day) = it

                        "$day.$month.$year"
                    }
                    else -> ""
                }
            }
        }

        addTableRowIfNotBlank(getString(R.string.fragment_about_occupation), data.occupation)
        addTableRowIfNotBlank(getString(R.string.fragment_about_interests), data.interests)
        addTableRowIfNotBlank(getString(R.string.fragment_about_city), data.city)
        addTableRowIfNotBlank(getString(R.string.fragment_about_country), data.country)
        addTableRowIfNotBlank(getString(R.string.fragment_about_gender), normalizedGender)
        addTableRowIfNotBlank(getString(R.string.fragment_about_relationship_status), normalizedRelationshipStatus)
        addTableRowIfNotBlank(getString(R.string.fragment_about_birthday), normalizedBirthday)
        addTableRowIfNotBlank(getString(R.string.fragment_about_website), data.website)
        addTableRowIfNotBlank(getString(R.string.fragment_about_facebook), data.facebook)
        addTableRowIfNotBlank(getString(R.string.fragment_about_youtube), data.youtube)
        addTableRowIfNotBlank(getString(R.string.fragment_about_chatango), data.chatango)
        addTableRowIfNotBlank(getString(R.string.fragment_about_twitter), data.twitter)
        addTableRowIfNotBlank(getString(R.string.fragment_about_skype), data.skype)
        addTableRowIfNotBlank(getString(R.string.fragment_about_deviantart), data.deviantart)
    }

    private fun addTableRowIfNotBlank(title: String, content: String) {
        val view = if (content.isNotBlank()) constructTableRow(title, content) else null

        if (view != null) generalTable.addView(view)
    }

    private fun constructTableRow(title: String, content: String): View {
        val tableRow = LayoutInflater.from(context).inflate(R.layout.layout_about_row, generalTable, false)
        val titleView = tableRow.findViewById<TextView>(R.id.title)
        val contentView = tableRow.findViewById<TextView>(R.id.content)

        contentView.setTextIsSelectable(true)
        contentView.isSaveEnabled = false

        titleView.text = title
        contentView.text = content.linkify(mentions = false)

        contentView.linkClicks()
            .map { it.toPrefixedUrlOrNull().toOptional() }
            .filterSome()
            .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
            .subscribe { showPage(it) }

        contentView.linkLongClicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                val clipboardTitle = getString(R.string.clipboard_title)

                requireContext().getSystemService<ClipboardManager>()?.setPrimaryClip(
                    ClipData.newPlainText(clipboardTitle, it.toString())
                )

                requireContext().toast(R.string.clipboard_status)
            }

        return tableRow
    }

    private fun constructHtmlSkeleton(content: String): String {
        return """
            <html>
              <head>
                <style>
                  body {
                    color: ${requireContext().resolveColor(android.R.attr.textColorSecondary).toHtmlColor()};
                  }
                  a {
                    color: ${requireContext().resolveColor(R.attr.colorLink).toHtmlColor()};
                  }
                </style>
              </head>
              <body>
                ${content.trim()}
              </body>
            </html>
            """.trimIndent()
    }

    private fun Int.toHtmlColor(): String {
        val red = this shr 16 and 0xff
        val green = this shr 8 and 0xff
        val blue = this and 0xff
        val alpha = this shr 24 and 0xff

        val normalizedAlpha = BigDecimal(alpha / 255.0).round(MathContext(2))

        return "rgba($red, $green, $blue, $normalizedAlpha)"
    }
}
