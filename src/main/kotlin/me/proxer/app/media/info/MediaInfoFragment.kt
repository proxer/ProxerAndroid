package me.proxer.app.media.info

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.industry.IndustryActivity
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.ui.view.MaxLineFlexboxLayout
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
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.MediaUserInfo
import me.proxer.library.enums.IndustryType
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class MediaInfoFragment : BaseContentFragment<Entry>(R.layout.fragment_media_info) {

    companion object {
        private const val SHOW_UNRATED_TAGS_ARGUMENT = "show_unrated_tags"
        private const val SHOW_SPOILER_TAGS_ARGUMENT = "show_spoiler_tags"

        fun newInstance() = MediaInfoFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    override val viewModel by sharedViewModel<MediaInfoViewModel> { parametersOf(id) }

    private val id: String
        get() = hostingActivity.id

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
    private val subscribeContainer: ViewGroup by bindView(R.id.subscribeContainer)
    private val subscribe: ImageView by bindView(R.id.subscribe)

    private val description: TextView by bindView(R.id.description)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUnratedButton()
        updateSpoilerButton()
        bindUserInfo(null)

        noteContainer.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.note() }

        favorContainer.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.markAsFavorite() }

        finishContainer.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.markAsFinished() }

        subscribeContainer.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                if (viewModel.userInfoData.value?.isSubscribed == true) {
                    viewModel.unsubscribe()
                } else {
                    viewModel.subscribe()
                }
            }

        unratedTags.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                showUnratedTags = !showUnratedTags

                viewModel.data.value?.let { entry -> bindTags(entry) }
            }

        spoilerTags.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                showSpoilerTags = !showSpoilerTags

                viewModel.data.value?.let { entry -> bindTags(entry) }
            }

        viewModel.userInfoData.observe(viewLifecycleOwner, Observer {
            bindUserInfo(it)
        })

        viewModel.userInfoUpdateData.observe(viewLifecycleOwner, Observer {
            it?.let {
                hostingActivity.snackbar(R.string.fragment_set_user_info_success)
            }
        })

        viewModel.userInfoUpdateError.observe(viewLifecycleOwner, Observer {
            it?.let {
                hostingActivity.multilineSnackbar(
                    getString(R.string.error_set_user_info, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                )
            }
        })
    }

    override fun showData(data: Entry) {
        super.showData(data)

        bind(data)
    }

    private fun bind(entry: Entry) {
        infoTable.removeAllViews()

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

        description.text = entry.description
    }

    private fun bindRating(result: Entry) = if (result.rating > 0) {
        ratingContainer.isVisible = true
        rating.rating = result.rating / 2.0f
        ratingAmount.isVisible = true
        ratingAmount.text = requireContext().resources.getQuantityString(
            R.plurals.fragment_media_info_rate_count,
            result.ratingAmount, result.rating, result.ratingAmount
        )
    } else {
        ratingContainer.isGone = true
        ratingAmount.isGone = true
    }

    private fun bindSynonyms(result: Entry) {
        result.synonyms.forEach {
            infoTable.addView(constructInfoTableRow(it.toTypeAppString(requireContext()), it.name, true))
        }
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
                seasonEndView.isGone = true
            }

            infoTable.addView(tableRow)
        }
    }

    private fun bindStatus(result: Entry) {
        infoTable.addView(
            constructInfoTableRow(
                requireContext().getString(R.string.fragment_media_info_status_title),
                result.state.toAppString(requireContext())
            )
        )
    }

    private fun bindLicense(result: Entry) {
        infoTable.addView(
            constructInfoTableRow(
                requireContext().getString(R.string.fragment_media_info_license_title),
                result.license.toAppString(requireContext())
            )
        )
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
                            .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                            .subscribe {
                                MediaActivity.navigateTo(
                                    requireActivity(),
                                    adaptionInfo.id, adaptionInfo.name, adaptionInfo.medium?.toCategory()
                                )
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
            genresTitle.isGone = true
            genres.isGone = true

            return
        }

        bindChips(
            genres,
            result.genres.toList(),
            mapFunction = { it.name },
            onClick = { hostingActivity.multilineSnackbar(it.description) }
        )
    }

    private fun bindTags(result: Entry) {
        if (result.tags.isEmpty()) {
            tagsTitle.isGone = true
            unratedTags.isGone = true
            spoilerTags.isGone = true
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

        bindChips(
            tags,
            filteredTags,
            mapFunction = { it.name },
            onClick = { hostingActivity.multilineSnackbar(it.description) }
        )
    }

    private fun updateUnratedButton() {
        unratedTags.text = getString(
            when (showUnratedTags) {
                true -> R.string.fragment_media_info_tags_unrated_hide
                false -> R.string.fragment_media_info_tags_unrated_show
            }
        )
    }

    private fun updateSpoilerButton() {
        spoilerTags.text = getString(
            when (showSpoilerTags) {
                true -> R.string.fragment_media_info_tags_spoiler_hide
                false -> R.string.fragment_media_info_tags_spoiler_show
            }
        )
    }

    private fun bindFskConstraints(result: Entry) {
        fskConstraints.removeAllViews()

        if (result.fskConstraints.isEmpty()) {
            fskConstraintsTitle.isGone = true
            fskConstraints.isGone = true
        } else {
            result.fskConstraints.forEach { constraint ->
                val image = LayoutInflater.from(context)
                    .inflate(R.layout.layout_image, fskConstraints, false) as ImageView

                image.setImageDrawable(constraint.toAppDrawable(requireContext()))

                image.clicks()
                    .autoDisposable(viewLifecycleOwner.scope())
                    .subscribe {
                        hostingActivity.multilineSnackbar(constraint.toAppStringDescription(requireContext()))
                    }

                fskConstraints.addView(image)
            }
        }
    }

    private fun bindTranslatorGroups(result: Entry) = if (result.translatorGroups.isEmpty()) {
        translatorGroupsTitle.isGone = true
        translatorGroups.isGone = true
    } else {
        bindChips(
            translatorGroups,
            result.translatorGroups,
            mapFunction = { it.name },
            onClick = { TranslatorGroupActivity.navigateTo(requireActivity(), it.id, it.name) }
        )
    }

    private fun bindIndustries(result: Entry) = if (result.industries.isEmpty()) {
        industriesTitle.isGone = true
        industries.isGone = true
    } else {
        bindChips(
            industries,
            result.industries,
            mapFunction = {
                if (it.type == IndustryType.UNKNOWN) {
                    it.name
                } else {
                    "${it.name} (${it.type.toAppString(requireContext())})"
                }
            },
            onClick = {
                IndustryActivity.navigateTo(requireActivity(), it.id, it.name)
            }
        )
    }

    @Suppress("LabeledExpression")
    private fun <T> bindChips(
        layout: FlexboxLayout,
        items: List<T>,
        mapFunction: (T) -> String = { it.toString() },
        onClick: ((T) -> Unit)? = null
    ) {
        layout.post {
            if (layout.width <= 0 || view == null) return@post
            if (layout.childCount > 0) layout.removeAllViews()

            for ((index, mappedItem) in items.asSequence().map(mapFunction).withIndex().toList()) {
                val chip = LayoutInflater.from(layout.context).inflate(R.layout.item_chip, layout, false) as Chip

                chip.text = mappedItem

                if (onClick != null) {
                    chip.clicks()
                        .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe { onClick.invoke(items[index]) }
                }

                if (layout is MaxLineFlexboxLayout && !layout.canAddView(chip)) {
                    layout.showAllEvents
                        .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                        .subscribe { bindChips(layout, items, mapFunction, onClick) }

                    layout.enableShowAllButton()

                    break
                } else {
                    layout.addView(chip)
                }
            }
        }
    }

    private fun bindUserInfo(userInfo: MediaUserInfo?) {
        val noteColor = if (userInfo?.isNoted == true) R.attr.colorSecondary else R.attr.colorIcon
        val favorColor = if (userInfo?.isTopTen == true) R.attr.colorSecondary else R.attr.colorIcon
        val finishColor = if (userInfo?.isFinished == true) R.attr.colorSecondary else R.attr.colorIcon
        val subscribeColor = if (userInfo?.isSubscribed == true) R.attr.colorSecondary else R.attr.colorIcon

        note.setIconicsImage(CommunityMaterial.Icon3.cmd_clock, 24, 0, noteColor)
        favor.setIconicsImage(CommunityMaterial.Icon.cmd_star, 24, 0, favorColor)
        finish.setIconicsImage(CommunityMaterial.Icon3.cmd_check, 24, 0, finishColor)
        subscribe.setIconicsImage(CommunityMaterial.Icon2.cmd_newspaper, 24, 0, subscribeColor)
    }
}
