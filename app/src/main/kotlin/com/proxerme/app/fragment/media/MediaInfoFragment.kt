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
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.library.connection.info.entity.Entry
import com.proxerme.library.connection.info.request.EntryRequest
import org.apmem.tools.layouts.FlowLayout

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
    private val japaneseTitle: TextView by bindView(R.id.japaneseTitle)
    private val japaneseTitleRow: TableRow by bindView(R.id.japaneseTitleRow)
    private val genres: FlowLayout by bindView(R.id.genres)
    private val fsk: FlowLayout by bindView(R.id.fsk)
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

                    "namejap" -> {
                        japaneseTitle.text = it.name
                        japaneseTitleRow.visibility = View.VISIBLE
                    }
                }
            }

            genres.removeAllViews()

            if (it.genres.isEmpty()) {
                genres.visibility = View.GONE
            } else {
                it.genres.forEach {
                    val imageView = layoutInflater.inflate(R.layout.item_badge, genres, false)
                            as ImageView

                    imageView.setImageDrawable(BadgeDrawable.Builder()
                            .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                            .badgeColor(ContextCompat.getColor(context, R.color.colorAccent))
                            .text1(it)
                            .build()
                            .apply {
                                setNeedAutoSetBounds(true)
                            })

                    genres.addView(imageView)
                }
            }

            fsk.removeAllViews()

            if (it.fsk.isEmpty()) {
                fsk.visibility = View.GONE
            } else {
                it.fsk.forEach {
                    val imageView = layoutInflater.inflate(R.layout.item_badge, fsk, false)
                            as ImageView

                    imageView.setImageDrawable(BadgeDrawable.Builder()
                            .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                            .badgeColor(ContextCompat.getColor(context, R.color.colorAccent))
                            .text1(it)
                            .build()
                            .apply {
                                setNeedAutoSetBounds(true)
                            })

                    fsk.addView(imageView)
                }
            }

            description.text = it.description
        }
    }
}