package me.proxer.app.media.info

import android.arch.lifecycle.Observer
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TableLayout
import android.widget.TextView
import com.gojuno.koptional.Optional
import com.google.android.flexbox.FlexboxLayout
import com.jakewharton.rxbinding2.view.clicks
import com.matrixxun.starry.badgetextview.MaterialBadgeTextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.industry.IndustryActivity
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.ui.view.MaxLineFlexboxLayout
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toAppStringDescription
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEndAppString
import me.proxer.app.util.extension.toStartAppString
import me.proxer.app.util.extension.toTypeAppString
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.MediaUserInfo
import me.proxer.library.enums.Category
import me.proxer.library.enums.IndustryType
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class MediaInfoFragment : BaseContentFragment<Pair<Entry, Optional<MediaUserInfo>>>() {

    companion object {
        private const val SHOW_UNRATED_TAGS_ARGUMENT = "show_unrated_tags"
        private const val SHOW_SPOILER_TAGS_ARGUMENT = "show_spoiler_tags"

        fun newInstance() = MediaInfoFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    override val viewModel by unsafeLazy { MediaInfoViewModelProvider.get(this, id) }

    private val id: String
        get() = hostingActivity.id

    private var name: String?
        get() = hostingActivity.name
        set(value) {
            hostingActivity.name = value
        }

    private var category: Category?
        get() = hostingActivity.category
        set(value) {
            hostingActivity.category = value
        }

    private var showUnratedTags: Boolean
        get() = requireArguments().getBoolean(SHOW_UNRATED_TAGS_ARGUMENT, false)
        set(value) = requireArguments().putBoolean(SHOW_UNRATED_TAGS_ARGUMENT, value)

    private var showSpoilerTags: Boolean
        get() = requireArguments().getBoolean(SHOW_SPOILER_TAGS_ARGUMENT, false)
        set(value) = requireArguments().putBoolean(SHOW_SPOILER_TAGS_ARGUMENT, value)

    private val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
    private val rating: RatingBar by bindView(R.id.rating)
    private val ratingAmount: TextView by bindView(R.id.ratingAmount)

    private val infoTable: TableLayout by bindView(R.id.infoTable)

    private val genresTitle: TextView by bindView(R.id.genresTitle)
    private val genres: FlexboxLayout by bindView(R.id.genres)
    private val tagsTitle: TextView by bindView(R.id.tagsTitle)
    private val tags: FlexboxLayout by bindView(R.id.tags)
    private val unratedTags: Button by bindView(R.id.unratedTags)
    private val spoilerTags: Button by bindView(R.id.spoilerTags)
    private val fskConstraintsTitle: TextView by bindView(R.id.fskConstraintsTitle)
    private val fskConstraints: FlexboxLayout by bindView(R.id.fskConstraints)
    private val translatorGroupsTitle: TextView by bindView(R.id.translatorGroupsTitle)
    private val translatorGroups: FlexboxLayout by bindView(R.id.translatorGroups)
    private val industriesTitle: TextView by bindView(R.id.industriesTitle)
    private val industries: FlexboxLayout by bindView(R.id.industries)

    private val noteContainer: ViewGroup by bindView(R.id.noteContainer)
    private val note: ImageView by bindView(R.id.note)
    private val favorContainer: ViewGroup by bindView(R.id.favorContainer)
    private val favor: ImageView by bindView(R.id.favor)
    private val finishContainer: ViewGroup by bindView(R.id.finishContainer)
    private val finish: ImageView by bindView(R.id.finish)

    private val description: TextView by bindView(R.id.description)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.userInfoUpdateData.observe(this, Observer {
            it?.let {
                snackbar(root, R.string.fragment_set_user_info_success)
            }
        })

        viewModel.userInfoUpdateError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_set_user_info, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity))
            }
        })

        noteContainer.clicks()
            .autoDispose(this)
            .subscribe { viewModel.note() }

        favorContainer.clicks()
            .autoDispose(this)
            .subscribe { viewModel.markAsFavorite() }

        finishContainer.clicks()
            .autoDispose(this)
            .subscribe { viewModel.markAsFinished() }

        unratedTags.clicks()
            .autoDispose(this)
            .subscribe {
                showUnratedTags = !showUnratedTags

                viewModel.data.value?.let { bindTags(it.first) }
            }

        spoilerTags.clicks()
            .autoDispose(this)
            .subscribe {
                showSpoilerTags = !showSpoilerTags

                viewModel.data.value?.let { bindTags(it.first) }
            }

        updateUnratedButton()
        updateSpoilerButton()
    }

    override fun showData(data: Pair<Entry, Optional<MediaUserInfo>>) {
        super.showData(data)

        name = data.first.name
        category = data.first.category

        bind(data.first, data.second)
    }

    private fun bind(entry: Entry, userInfo: Optional<MediaUserInfo>) {
        bindRating(entry)
        bindSynonyms(entry)
        bindSeasons(entry)
        bindStatus(entry)
        bindLicense(entry)
        bindAdaption(entry)
        bindGenres(entry)
        bindTags(entry)
        bindFskConstraints(entry)
        bindTranslatorGroups(entry)
        bindIndustries(entry)
        bindUserInfo(userInfo)

        description.text = entry.description
    }

    private fun bindRating(result: Entry) = if (result.rating > 0) {
        ratingContainer.visibility = View.VISIBLE
        rating.rating = result.rating / 2.0f
        ratingAmount.visibility = View.VISIBLE
        ratingAmount.text = requireContext().resources.getQuantityString(R.plurals.fragment_media_info_rate_count,
            result.ratingAmount, result.rating, result.ratingAmount)
    } else {
        ratingContainer.visibility = View.GONE
        ratingAmount.visibility = View.GONE
    }

    private fun bindSynonyms(result: Entry) = result.synonyms.forEach {
        infoTable.addView(constructInfoTableRow(it.toTypeAppString(requireContext()), it.name, true))
    }

    private fun bindSeasons(result: Entry) {
        val seasons = result.seasons

        if (seasons.isNotEmpty()) {
            val tableRow = LayoutInflater.from(context)
                .inflate(R.layout.layout_media_info_seasons_row, infoTable, false)

            val seasonStartView = tableRow.findViewById<TextView>(R.id.seasonStart)
            val seasonEndView = tableRow.findViewById<TextView>(R.id.seasonEnd)

            seasonStartView.text = seasons[0].toStartAppString(requireContext())

            if (seasons.size >= 2) {
                seasonEndView.text = seasons[1].toEndAppString(requireContext())
            } else {
                seasonEndView.visibility = View.GONE
            }

            infoTable.addView(tableRow)
        }
    }

    private fun bindStatus(result: Entry) {
        infoTable.addView(constructInfoTableRow(requireContext().getString(R.string.fragment_media_info_status_title),
            result.state.toAppString(requireContext())))
    }

    private fun bindLicense(result: Entry) {
        infoTable.addView(constructInfoTableRow(requireContext().getString(R.string.fragment_media_info_license_title),
            result.license.toAppString(requireContext())))
    }

    private fun bindAdaption(result: Entry) {
        result.adaptionInfo.let { adaptionInfo ->
            if (adaptionInfo.id != "0") {
                val title = getString(R.string.fragment_media_info_adaption_title)
                val content = "${adaptionInfo.name} (${adaptionInfo.medium?.toAppString(requireContext())})"

                infoTable.addView(constructInfoTableRow(title, content).also { tableRow ->
                    tableRow.findViewById<View>(R.id.content).also { contentView ->
                        val selectableItemBackground = TypedValue().apply {
                            requireContext().theme.resolveAttribute(R.attr.selectableItemBackground, this, true)
                        }

                        contentView.setBackgroundResource(selectableItemBackground.resourceId)
                        contentView.clicks()
                            .autoDispose(this)
                            .subscribe {
                                MediaActivity.navigateTo(requireActivity(), adaptionInfo.id, adaptionInfo.name,
                                    adaptionInfo.medium?.toCategory())
                            }
                    }
                })
            }
        }
    }

    private fun constructInfoTableRow(title: String, content: String, isSelectable: Boolean = false): View {
        val tableRow = LayoutInflater.from(context).inflate(R.layout.layout_media_info_row, infoTable, false)
        val titleView = tableRow.findViewById<TextView>(R.id.title)
        val contentView = tableRow.findViewById<TextView>(R.id.content)

        titleView.text = title
        contentView.text = content
        contentView.isSaveEnabled = false
        contentView.setTextIsSelectable(isSelectable)

        return tableRow
    }

    private fun bindGenres(result: Entry) {
        if (result.genres.isEmpty()) {
            genresTitle.visibility = View.GONE
            genres.visibility = View.GONE

            return
        }

        bindChips(genres, result.genres.toList(),
            mapFunction = {
                ProxerUtils.getApiEnumName(it)
                    ?: throw IllegalArgumentException("Unknown genre: $it")
            },
            onClick = {
                showPage(ProxerUrls.wikiWeb(ProxerUtils.getApiEnumName(it)
                    ?: throw IllegalArgumentException("Unknown genre: $it")))
            })
    }

    private fun bindTags(result: Entry) {
        if (result.tags.isEmpty()) {
            tagsTitle.visibility = View.GONE
            unratedTags.visibility = View.GONE
            spoilerTags.visibility = View.GONE
        } else {
            updateSpoilerButton()
            updateUnratedButton()
        }

        val filteredTags = result.tags.filter {
            when (it.isRated) {
                true -> when (it.isSpoiler) {
                    true -> showSpoilerTags
                    false -> true
                }
                false -> when (showUnratedTags) {
                    true -> when (it.isSpoiler) {
                        true -> showSpoilerTags
                        false -> true
                    }
                    false -> false
                }
            }
        }

        bindChips(tags, filteredTags,
            mapFunction = { it.name },
            onClick = { multilineSnackbar(root, it.description) })
    }

    private fun updateUnratedButton() {
        unratedTags.text = getString(when (showUnratedTags) {
            true -> R.string.fragment_media_info_tags_unrated_hide
            false -> R.string.fragment_media_info_tags_unrated_show
        })
    }

    private fun updateSpoilerButton() {
        spoilerTags.text = getString(when (showSpoilerTags) {
            true -> R.string.fragment_media_info_tags_spoiler_hide
            false -> R.string.fragment_media_info_tags_spoiler_show
        })
    }

    private fun bindFskConstraints(result: Entry) = if (result.fskConstraints.isEmpty()) {
        fskConstraintsTitle.visibility = View.GONE
        fskConstraints.visibility = View.GONE
    } else {
        result.fskConstraints.forEach { constraint ->
            val image = LayoutInflater.from(context)
                .inflate(R.layout.layout_image, fskConstraints, false) as ImageView

            image.setImageDrawable(constraint.toAppDrawable(requireContext()))
            image.setOnClickListener {
                multilineSnackbar(root, constraint.toAppStringDescription(requireContext()))
            }

            fskConstraints.addView(image)
        }
    }

    private fun bindTranslatorGroups(result: Entry) = if (result.translatorGroups.isEmpty()) {
        translatorGroupsTitle.visibility = View.GONE
        translatorGroups.visibility = View.GONE
    } else {
        bindChips(translatorGroups, result.translatorGroups,
            mapFunction = { it.name },
            onClick = { TranslatorGroupActivity.navigateTo(requireActivity(), it.id, it.name) })
    }

    private fun bindIndustries(result: Entry) = if (result.industries.isEmpty()) {
        industriesTitle.visibility = View.GONE
        industries.visibility = View.GONE
    } else {
        bindChips(industries, result.industries,
            mapFunction = {
                if (it.type == IndustryType.UNKNOWN) {
                    it.name
                } else {
                    "${it.name} (${it.type.toAppString(requireContext())})"
                }
            },
            onClick = { IndustryActivity.navigateTo(requireActivity(), it.id, it.name) })
    }

    private fun <T> bindChips(
        layout: FlexboxLayout,
        items: List<T>,
        mapFunction: (T) -> String = { it.toString() },
        onClick: ((T) -> Unit)? = null
    ) {
        layout.post {
            if (layout.childCount > 0) layout.removeAllViews()

            for ((index, mappedItem) in items.map(mapFunction).withIndex()) {
                val badge = MaterialBadgeTextView(layout.context)

                badge.text = mappedItem
                badge.setTypeface(null, Typeface.BOLD)
                badge.setTextColor(ContextCompat.getColorStateList(layout.context, android.R.color.white))
                badge.setBackgroundColor(ContextCompat.getColor(badge.context, R.color.colorAccent))
                badge.setOnClickListener { onClick?.invoke(items[index]) }

                if (layout is MaxLineFlexboxLayout && !layout.canAddView(badge)) {
                    layout.enableShowAllButton {
                        layout.maxLines = Int.MAX_VALUE

                        bindChips(layout, items, mapFunction, onClick)
                    }

                    break
                } else {
                    layout.addView(badge)
                }
            }
        }
    }

    private fun bindUserInfo(userInfo: Optional<MediaUserInfo>) {
        userInfo.toNullable().let { nullableUserInfo ->
            val noteColor = if (nullableUserInfo?.isNoted == true) R.color.colorAccent else R.color.icon
            val favorColor = if (nullableUserInfo?.isTopTen == true) R.color.colorAccent else R.color.icon
            val finishColor = if (nullableUserInfo?.isFinished == true) R.color.colorAccent else R.color.icon

            note.setIconicsImage(CommunityMaterial.Icon.cmd_clock, 24, 0, noteColor)
            favor.setIconicsImage(CommunityMaterial.Icon.cmd_star, 24, 0, favorColor)
            finish.setIconicsImage(CommunityMaterial.Icon.cmd_check, 24, 0, finishColor)
        }
    }
}
