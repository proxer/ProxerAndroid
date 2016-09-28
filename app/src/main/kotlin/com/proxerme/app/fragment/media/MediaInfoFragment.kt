package com.proxerme.app.fragment.media

import android.os.Bundle
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
                seasonStart.text = "Start: ${getSeasonString(it.seasons[0])}"

                if (it.seasons.size >= 2) {
                    seasonEnd.text = "Ende: ${getSeasonString(it.seasons[1])}"
                } else {
                    seasonEnd.visibility = View.GONE
                }
            } else {
                seasonsRow.visibility = View.GONE
            }

            status.text = getStateString(it.state)

            license.text = when (it.license) {
                LicenseParameter.LICENSED -> "Lizensiert"
                LicenseParameter.NON_LICENSED -> "Nicht Lizensiert"
                LicenseParameter.UNKNOWN -> "Unbekannt"
                else -> throw InvalidParameterException("Unknwon license: " + it.license)
            }

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

                    imageView.setImageDrawable(BadgeDrawable.Builder()
                            .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                            .badgeColor(ContextCompat.getColor(context, R.color.colorAccent))
                            .text1(it)
                            .textSize(Utils.convertSpToPx(context, 14f))
                            .build()
                            .apply {
                                setNeedAutoSetBounds(true)
                            })

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
        return when (state) {
            StateParameter.PRE_AIRING -> "Nicht erschienen (Pre-Airing)"
            StateParameter.AIRING -> "Airing"
            StateParameter.CANCELLED -> "Abgebrochen"
            StateParameter.CANCELLED_SUB -> "Abgebrochener Sub"
            StateParameter.FINISHED -> "Abgeschlossen"
            else -> throw IllegalArgumentException("Unknwon state: $state")
        }
    }

    private fun getSeasonString(season: EntrySeason): String {
        return "${when (season.season) {
            SeasonParameter.WINTER -> "Winter"
            SeasonParameter.SPRING -> "Frühling"
            SeasonParameter.SUMMER -> "Sommer"
            SeasonParameter.AUTUMN -> "Herbst"
            else -> throw IllegalArgumentException("Unknwon season: ${season.season}")
        }} ${season.year}"
    }
}