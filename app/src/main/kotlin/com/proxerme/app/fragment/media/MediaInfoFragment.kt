package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import butterknife.bindView
import cn.nekocode.badge.BadgeDrawable
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.info.entity.Entry
import com.proxerme.library.connection.info.entity.EntrySeason
import com.proxerme.library.connection.info.entity.Publisher
import com.proxerme.library.connection.info.request.EntryRequest
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.FskParameter
import com.proxerme.library.parameters.LicenseParameter
import com.proxerme.library.parameters.SeasonParameter
import com.proxerme.library.parameters.StateParameter
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
    private var entry: Entry? = null

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
        entry = savedInstanceState?.getParcelable(STATE_ENTRY)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media_info, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(STATE_ENTRY, entry)
    }

    override fun clear() {
        entry = null
    }

    override fun constructLoadingRequest(): LoadingRequest<Entry> {
        return LoadingRequest(EntryRequest(id))
    }

    override fun save(result: Entry) {
        entry = result

        (activity as MediaActivity).setName(result.name)
    }

    override fun show() {
        entry?.let {
            val layoutInflater = LayoutInflater.from(context)

            it.synonyms.forEach {
                when (it.type) {
                    "name" -> {
                        originalTitle.text = it.name
                        originalTitleRow.visibility = View.VISIBLE
                    }

                    "nameeng" -> {
                        englishTitle.text = it.name
                        englishTitleRow.visibility = View.VISIBLE
                    }

                    "nameger" -> {
                        germanTitle.text = it.name
                        germanTitleRow.visibility = View.VISIBLE
                    }

                    "namejap" -> {
                        japaneseTitle.text = it.name
                        japaneseTitleRow.visibility = View.VISIBLE
                    }
                }
            }

            if (it.seasons.size >= 1) {
                seasonStart.text = getSeasonStartString(it.seasons[0])

                if (it.seasons.size >= 2) {
                    seasonEnd.text = getSeasonEndString(it.seasons[1])
                } else {
                    seasonEnd.visibility = View.GONE
                }
            } else {
                seasonsRow.visibility = View.GONE
            }

            status.text = getStateString(it.state)

            license.text = getString(when (it.license) {
                LicenseParameter.LICENSED -> R.string.media_license_licensed
                LicenseParameter.NON_LICENSED -> R.string.media_license_non_licensed
                LicenseParameter.UNKNOWN -> R.string.media_license_unknown
                else -> throw InvalidParameterException("Unknown license: " + it.license)
            })

            buildBadgeView(genresTitle, genres, it.genres, { it }, {
                ProxerUrlHolder.getWikiUrl(it).toString()
            })

            fsk.removeAllViews()

            if (it.fsk.isEmpty()) {
                fskTitle.visibility = View.GONE
                fsk.visibility = View.GONE
            } else {
                it.fsk.forEach {
                    val imageView = layoutInflater.inflate(R.layout.item_badge, fsk, false)
                            as ImageView

                    imageView.setImageResource(getFskImage(it))

                    fsk.addView(imageView)
                }
            }

            buildBadgeView(groupsTitle, groups, it.subgroups, { it.name }, {
                ProxerUrlHolder.getSubgroupUrl(it.id,
                        ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT).toString()
            })

            buildBadgeView(publishersTitle, publishers, it.publishers, {
                getPublisherString(it)
            }, {
                ProxerUrlHolder.getPublisherUrl(it.id,
                        ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT).toString()
            })

            description.text = it.description
        }
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

    private fun <T> buildBadgeView(title: TextView, badgeContainer: ViewGroup,
                                   items: Array<T>, transform: (T) -> String, url: (T) -> String) {
        badgeContainer.removeAllViews()

        if (items.isEmpty()) {
            title.visibility = View.GONE
            badgeContainer.visibility = View.GONE
        } else {
            val inflater = LayoutInflater.from(context)

            items.forEach { item ->
                val imageView = inflater.inflate(R.layout.item_badge, badgeContainer, false)
                        as ImageView

                imageView.setImageDrawable(BadgeDrawable.Builder()
                        .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                        .badgeColor(ContextCompat.getColor(context, R.color.colorAccent))
                        .text1(transform.invoke(item))
                        .textSize(Utils.convertSpToPx(context, 14f))
                        .build()
                        .apply {
                            setNeedAutoSetBounds(true)
                        })
                imageView.setOnClickListener {
                    Utils.viewLink(context, url.invoke(item))
                }

                badgeContainer.addView(imageView)
            }
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
        return getString(when (season.season) {
            SeasonParameter.WINTER -> R.string.fragment_media_season_winter_start
            SeasonParameter.SPRING -> R.string.fragment_media_season_spring_start
            SeasonParameter.SUMMER -> R.string.fragment_media_season_summer_start
            SeasonParameter.AUTUMN -> R.string.fragment_media_season_autumn_start
            else -> throw IllegalArgumentException("Unknown season: ${season.season}")
        }, season.year)
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