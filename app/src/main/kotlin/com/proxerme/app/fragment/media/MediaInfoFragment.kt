package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.info.entity.*
import com.proxerme.library.connection.info.request.EntryRequest
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.*
import org.apmem.tools.layouts.FlowLayout
import java.security.InvalidParameterException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaInfoFragment : EasyLoadingFragment<Entry>() {

    companion object {
        private const val ARGUMENT_ID = "id"
        private const val STATE_ENTRY = "state_entry"
        private const val STATE_SHOW_UNRATED_TAGS = "state_unrated_tags"
        private const val STATE_SHOW_SPOILER_TAGS = "state_spoiler_tags"

        fun newInstance(id: String): MediaInfoFragment {
            return MediaInfoFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.MEDIA_INFO

    private lateinit var id: String

    private var showUnratedTags: Boolean = false
    private var showSpoilerTags: Boolean = false

    private val root: ViewGroup by bindView(R.id.root)

    private val originalTitle: TextView by bindView(R.id.originalTitle)
    private val originalTitleRow: TableRow by bindView(R.id.originalTitleRow)
    private val englishTitle: TextView by bindView(R.id.englishTitle)
    private val englishTitleRow: TableRow by bindView(R.id.englishTitleRow)
    private val germanTitle: TextView by bindView(R.id.germanTitle)
    private val germanTitleRow: TableRow by bindView(R.id.germanTitleRow)
    private val japaneseTitle: TextView by bindView(R.id.japaneseTitle)
    private val japaneseTitleRow: TableRow by bindView(R.id.japaneseTitleRow)
    private val seasonStart: TextView by bindView(R.id.seasonStart)
    private val seasonEnd: TextView by bindView(R.id.seasonEnd)
    private val seasonsRow: TableRow by bindView(R.id.seasonsRow)
    private val status: TextView by bindView(R.id.status)
    private val license: TextView by bindView(R.id.license)

    private val genres: FlowLayout by bindView(R.id.genres)
    private val genresTitle: TextView by bindView(R.id.genresTitle)
    private val tags: FlowLayout by bindView(R.id.tags)
    private val tagsTitle: TextView by bindView(R.id.tagsTitle)
    private val unratedTagsButton: Button by bindView(R.id.unratedTagsButton)
    private val spoilerTagsButton: Button by bindView(R.id.spoilerTagsButton)
    private val fsk: FlowLayout by bindView(R.id.fsk)
    private val fskTitle: TextView  by bindView(R.id.fskTitle)
    private val groups: FlowLayout by bindView(R.id.groups)
    private val groupsTitle: TextView by bindView(R.id.groupsTitle)
    private val publishers: FlowLayout by bindView(R.id.publishers)
    private val publishersTitle: TextView by bindView(R.id.publishersTitle)
    private val description: TextView by bindView(R.id.description)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments.getString(ARGUMENT_ID)

        if (savedInstanceState != null) {
            result = savedInstanceState.getParcelable(STATE_ENTRY)
            showUnratedTags = savedInstanceState.getBoolean(STATE_SHOW_UNRATED_TAGS)
            showSpoilerTags = savedInstanceState.getBoolean(STATE_SHOW_SPOILER_TAGS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_info, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(STATE_ENTRY, result)
        outState.putBoolean(STATE_SHOW_UNRATED_TAGS, showUnratedTags)
        outState.putBoolean(STATE_SHOW_SPOILER_TAGS, showSpoilerTags)
    }

    override fun showContent(result: Entry) {
        (activity as MediaActivity).setName(result.name)

        buildSynonymsView(result.synonyms)
        buildSeasonsView(result.seasons)

        status.text = getStateString(result.state)
        license.text = getString(when (result.license) {
            LicenseParameter.LICENSED -> R.string.media_license_licensed
            LicenseParameter.NON_LICENSED -> R.string.media_license_non_licensed
            LicenseParameter.UNKNOWN -> R.string.media_license_unknown
            else -> throw InvalidParameterException("Unknown license: " + result.license)
        })

        buildBadgeView(genres, result.genres, { it }, { view, genre ->
            (activity as MediaActivity).showPage(ProxerUrlHolder.getWikiUrl(genre))
        }, genresTitle)

        buildTagsView(result.tags)
        buildFskView(result.fsk)

        buildBadgeView(groups, result.subgroups, { it.name }, { view, subgroup ->
            (activity as MediaActivity).showPage(ProxerUrlHolder.getSubgroupUrl(subgroup.id,
                    ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT))
        }, groupsTitle)

        buildBadgeView(publishers, result.publishers, {
            getPublisherString(it)
        }, { view, publisher ->
            (activity as MediaActivity).showPage(ProxerUrlHolder.getPublisherUrl(publisher.id,
                    ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT))
        }, publishersTitle)

        description.text = result.description
    }

    override fun constructLoadingRequest(): LoadingRequest<Entry> {
        return LoadingRequest(EntryRequest(id))
    }

    private fun buildSynonymsView(synonyms: Array<Synonym>) {
        synonyms.forEach {
            when (it.type) {
                SynonymTypeParameter.ORIGINAL -> {
                    originalTitle.text = it.name
                    originalTitleRow.visibility = View.VISIBLE
                }

                SynonymTypeParameter.ENGLISH -> {
                    englishTitle.text = it.name
                    englishTitleRow.visibility = View.VISIBLE
                }

                SynonymTypeParameter.GERMAN -> {
                    germanTitle.text = it.name
                    germanTitleRow.visibility = View.VISIBLE
                }

                SynonymTypeParameter.JAPANESE -> {
                    japaneseTitle.text = it.name
                    japaneseTitleRow.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun buildSeasonsView(seasons: Array<EntrySeason>) {
        if (seasons.isNotEmpty()) {
            seasonStart.text = getSeasonStartString(seasons[0])

            if (seasons.size >= 2) {
                seasonEnd.text = getSeasonEndString(seasons[1])
            } else {
                seasonEnd.visibility = View.GONE
            }
        } else {
            seasonsRow.visibility = View.GONE
        }
    }

    private fun buildTagsView(tagArray: Array<Tag>) {
        if (tagArray.isEmpty()) {
            tagsTitle.visibility = View.GONE
            unratedTagsButton.visibility = View.GONE
            spoilerTagsButton.visibility = View.GONE
        } else {
            tagsTitle.visibility = View.VISIBLE

            updateUnratedButton()
            unratedTagsButton.setOnClickListener {
                showUnratedTags = !showUnratedTags

                result?.let {
                    buildTagsView(it.tags)
                }
            }

            updateSpoilerButton()
            spoilerTagsButton.setOnClickListener {
                showSpoilerTags = !showSpoilerTags

                result?.let {
                    buildTagsView(it.tags)
                }
            }
        }

        buildBadgeView(tags, tagArray.filter {
            if (it.isRated) {
                if (it.isSpoiler) {
                    showSpoilerTags
                } else {
                    true
                }
            } else {
                if (showUnratedTags) {
                    if (it.isSpoiler) {
                        showSpoilerTags
                    } else {
                        true
                    }
                } else {
                    false
                }
            }
        }.toTypedArray(), { it.name }, { view, tag ->
            Utils.makeMultilineSnackbar(root, tag.description, Snackbar.LENGTH_LONG).show()
        })
    }

    private fun updateUnratedButton() {
        unratedTagsButton.text = getString(when (showUnratedTags) {
            true -> R.string.tags_unrated_hide
            false -> R.string.tags_unrated_show
        })
    }

    private fun updateSpoilerButton() {
        spoilerTagsButton.text = getString(when (showSpoilerTags) {
            true -> R.string.tags_spoiler_hide
            false -> R.string.tags_spoiler_show
        })
    }

    private fun buildFskView(fskEntries: Array<String>) {
        fsk.removeAllViews()

        if (fskEntries.isEmpty()) {
            fskTitle.visibility = View.GONE
            fsk.visibility = View.GONE
        } else {
            fskEntries.forEach { fskEntry ->
                val imageView = LayoutInflater.from(context).inflate(R.layout.item_badge,
                        fsk, false) as ImageView

                imageView.setImageResource(getFskImage(fskEntry))
                imageView.setOnClickListener {
                    Utils.makeMultilineSnackbar(root, getFskDescription(fskEntry),
                            Snackbar.LENGTH_LONG).show()
                }

                fsk.addView(imageView)
            }
        }
    }

    private fun getFskDescription(fsk: String): String {
        return getString(when (fsk) {
            FskParameter.FSK_0 -> R.string.fsk_0_description
            FskParameter.FSK_6 -> R.string.fsk_6_description
            FskParameter.FSK_12 -> R.string.fsk_12_description
            FskParameter.FSK_16 -> R.string.fsk_16_description
            FskParameter.FSK_18 -> R.string.fsk_18_description
            FskParameter.BAD_LANGUAGE -> R.string.fsk_bad_language_description
            FskParameter.FEAR -> R.string.fsk_fear_description
            FskParameter.SEX -> R.string.fsk_sex_description
            FskParameter.VIOLENCE -> R.string.fsk_violence_description
            else -> throw IllegalArgumentException("Unknown fsk: $fsk")
        })
    }

    @DrawableRes
    private fun getFskImage(fsk: String): Int {
        return when (fsk) {
            FskParameter.FSK_0 -> R.drawable.ic_fsk0
            FskParameter.FSK_6 -> R.drawable.ic_fsk6
            FskParameter.FSK_12 -> R.drawable.ic_fsk12
            FskParameter.FSK_16 -> R.drawable.ic_fsk16
            FskParameter.FSK_18 -> R.drawable.ic_fsk18
            FskParameter.BAD_LANGUAGE -> R.drawable.ic_bad_language
            FskParameter.FEAR -> R.drawable.ic_fear
            FskParameter.SEX -> R.drawable.ic_sex
            FskParameter.VIOLENCE -> R.drawable.ic_violence
            else -> throw IllegalArgumentException("Unknown fsk: $fsk")
        }
    }

    private fun <T> buildBadgeView(badgeContainer: ViewGroup, items: Array<T>,
                                   transform: (T) -> String, onClick: ((View, T) -> Unit)? = null,
                                   vararg viewsToHideIfEmpty: View) {
        badgeContainer.removeAllViews()

        if (items.isEmpty()) {
            badgeContainer.visibility = View.GONE
            viewsToHideIfEmpty.forEach {
                it.visibility = View.GONE
            }
        } else {
            badgeContainer.visibility = View.VISIBLE
            viewsToHideIfEmpty.forEach {
                it.visibility = View.VISIBLE
            }

            Utils.populateBadgeView(badgeContainer, items, transform, onClick)
        }
    }

    private fun getPublisherString(publisher: Publisher): String {
        return "${publisher.name} (${publisher.type.capitalize()})"
    }

    private fun getStateString(state: Int): String {
        return getString(when (state) {
            StateParameter.PRE_AIRING -> R.string.media_state_pre_airing
            StateParameter.AIRING -> R.string.media_state_airing
            StateParameter.CANCELLED -> R.string.media_state_cancelled
            StateParameter.CANCELLED_SUB -> R.string.media_state_cancelled_sub
            StateParameter.FINISHED -> R.string.media_state_finished
            else -> throw IllegalArgumentException("Unknown state: $state")
        })
    }

    private fun getSeasonStartString(season: EntrySeason): String {
        return when (season.season) {
            SeasonParameter.WINTER -> getString(R.string.fragment_media_season_winter_start,
                    season.year)
            SeasonParameter.SPRING -> getString(R.string.fragment_media_season_spring_start,
                    season.year)
            SeasonParameter.SUMMER -> getString(R.string.fragment_media_season_summer_start,
                    season.year)
            SeasonParameter.AUTUMN -> getString(R.string.fragment_media_season_autumn_start,
                    season.year)
            SeasonParameter.UNSPECIFIED -> season.year.toString()
            else -> throw IllegalArgumentException("Unknown season: ${season.season}")
        }
    }

    private fun getSeasonEndString(season: EntrySeason): String {
        return getString(when (season.season) {
            SeasonParameter.WINTER -> R.string.fragment_media_season_winter_end
            SeasonParameter.SPRING -> R.string.fragment_media_season_spring_end
            SeasonParameter.SUMMER -> R.string.fragment_media_season_summer_end
            SeasonParameter.AUTUMN -> R.string.fragment_media_season_autumn_end
            else -> throw IllegalArgumentException("Unknown season: ${season.season}")
        }, season.year)
    }
}