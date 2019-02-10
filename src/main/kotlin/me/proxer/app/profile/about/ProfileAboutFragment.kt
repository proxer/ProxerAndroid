package me.proxer.app.profile.about

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TableLayout
import android.widget.TextView
import androidx.annotation.ContentView
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import linkClicks
import linkLongClicks
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.resolveColor
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toast
import me.proxer.library.entity.user.UserAbout
import me.proxer.library.enums.Gender
import me.proxer.library.enums.RelationshipStatus
import org.koin.androidx.viewmodel.ext.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
@ContentView(R.layout.fragment_about)
class ProfileAboutFragment : BaseContentFragment<UserAbout>() {

    companion object {
        private const val ZERO_DATE = "0000-00-00"

        fun newInstance() = ProfileAboutFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: ProfileActivity
        get() = activity as ProfileActivity

    override val viewModel by viewModel<ProfileAboutViewModel> {
        parametersOf(userId.toOptional(), username.toOptional())
    }

    private val userId: String?
        get() = hostingActivity.userId

    private val username: String?
        get() = hostingActivity.username

    private val generalContainer by bindView<ViewGroup>(R.id.generalContainer)
    private val generalTable by bindView<TableLayout>(R.id.generalTable)
    private val aboutContainer by bindView<ViewGroup>(R.id.aboutContainer)
    private val about by bindView<WebView>(R.id.about)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        about.setBackgroundColor(Color.TRANSPARENT)
        about.setInitialScale(1)
        about.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        about.settings.userAgentString = USER_AGENT
        about.settings.loadWithOverviewMode = true
        about.settings.javaScriptEnabled = false
        about.settings.useWideViewPort = true
        about.settings.defaultFontSize = 22
        about.isHorizontalScrollBarEnabled = false
        about.isVerticalScrollBarEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            about.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        }

        about.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                val httpUrl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Utils.parseAndFixUrl(request.url.toString())
                } else {
                    Utils.parseAndFixUrl(request.toString())
                }

                return if (httpUrl != null) {
                    showPage(httpUrl)

                    true
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
        }
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
                true -> about.loadData(constructHtmlSkeleton(it), "text/html", "utf-8")
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
            .map { Utils.parseAndFixUrl(it).toOptional() }
            .filterSome()
            .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
            .subscribe { showPage(it) }

        contentView.linkLongClicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                val clipboardTitle = getString(R.string.clipboard_title)

                requireContext().getSystemService<ClipboardManager>()?.primaryClip =
                    ClipData.newPlainText(clipboardTitle, it)

                requireContext().toast(R.string.clipboard_status)
            }

        return tableRow
    }

    private fun constructHtmlSkeleton(content: String): String {
        return """
            <html>
              <head>
                <style>
                  body { color: ${requireContext().resolveColor(android.R.attr.textColorSecondary).toHtmlColor()} }
                  a { color: ${requireContext().resolveColor(R.attr.colorLink).toHtmlColor()} }
                </style>
              </head>
              <body>
              ${content.trim()}
              </body>
            </html>
            """
    }

    private fun Int.toHtmlColor(): String {
        val red = this shr 16 and 0xff
        val green = this shr 8 and 0xff
        val blue = this and 0xff
        val alpha = this shr 24 and 0xff

        return "rgba($red, $green, $blue, $alpha)"
    }
}
