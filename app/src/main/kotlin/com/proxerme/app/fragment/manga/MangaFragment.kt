package com.proxerme.app.fragment.manga

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.activity.MangaActivity
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.manga.MangaAdapter
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.manga.entity.Chapter
import com.proxerme.library.connection.manga.request.ChapterRequest
import com.proxerme.library.info.ProxerUrlHolder
import org.joda.time.DateTime

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MangaFragment : EasyLoadingFragment<Chapter>() {

    companion object {
        private const val ARGUMENT_ID = "id"
        private const val ARGUMENT_EPISODE = "episode"
        private const val ARGUMENT_TOTAL_EPISODES = "total_episodes"
        private const val ARGUMENT_LANGUAGE = "language"

        private const val CHAPTER_STATE = "chapter_state"

        private const val DATE_PATTERN = "dd.MM.yyyy"

        fun newInstance(id: String, episode: Int, totalEpisodes: Int, language: String):
                MangaFragment {
            return MangaFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                    this.putInt(ARGUMENT_EPISODE, episode)
                    this.putInt(ARGUMENT_TOTAL_EPISODES, totalEpisodes)
                    this.putString(ARGUMENT_LANGUAGE, language)
                }
            }
        }
    }

    override val section = SectionManager.Section.MANGA

    private val id: String
        get() = arguments.getString(ARGUMENT_ID)
    private val totalEpisodes: Int
        get() = arguments.getInt(ARGUMENT_TOTAL_EPISODES)
    private val episode: Int
        get() = arguments.getInt(ARGUMENT_EPISODE)
    private val language: String
        get() = arguments.getString(ARGUMENT_LANGUAGE)

    private lateinit var adapter: MangaAdapter

    private var chapter: Chapter? = null

    private val uploader: TextView by bindView(R.id.uploader)
    private val scangroup: TextView by bindView(R.id.scangroup)
    private val date: TextView by bindView(R.id.date)
    private val previous: Button by bindView(R.id.previous)
    private val next: Button by bindView(R.id.next)
    private val pages: RecyclerView by bindView(R.id.pages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            chapter = it.getParcelable(CHAPTER_STATE)
        }

        adapter = MangaAdapter(savedInstanceState)
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?,
                              savedInstanceState: android.os.Bundle?): android.view.View {
        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pages.isNestedScrollingEnabled = false
        pages.layoutManager = LinearLayoutManager(context)
        pages.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(CHAPTER_STATE, chapter)
        adapter.saveInstanceState(outState)
    }

    override fun save(result: Chapter) {
        chapter = result

        adapter.init(result.server, result.entryId, result.id)
        adapter.replace(result.pages)
    }

    override fun show() {
        chapter?.let {
            uploader.text = it.uploader

            if (it.scangroup == null) {
                scangroup.text = context.getString(R.string.fragment_manga_empty_scangroup)
            } else {
                scangroup.text = it.scangroup
            }

            uploader.setOnClickListener { view ->
                UserActivity.navigateTo(activity, it.uploaderId, it.uploader)
            }

            scangroup.setOnClickListener { view ->
                it.scangroup?.let {
                    Utils.viewLink(context, ProxerUrlHolder.getSubgroupUrl(it,
                            ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT).toString())
                }
            }

            date.text = DateTime(it.time * 1000).toString(DATE_PATTERN)

            if (episode <= 1) {
                previous.visibility = View.GONE
            } else {
                previous.visibility = View.VISIBLE
                previous.text = getString(R.string.fragment_manga_previous_chapter)
                previous.setOnClickListener {
                    switchEpisode(episode - 1)
                }
            }

            if (episode >= totalEpisodes) {
                next.visibility = View.GONE
            } else {
                next.visibility = View.VISIBLE
                next.text = getString(R.string.fragment_manga_next_chapter)
                next.setOnClickListener {
                    switchEpisode(episode + 1)
                }
            }
        }
    }

    override fun clear() {
        chapter = null
        adapter.clear()
    }

    override fun constructLoadingRequest(): LoadingRequest<Chapter> {
        return LoadingRequest(ChapterRequest(id, episode, language))
    }

    private fun switchEpisode(newEpisode: Int) {
        arguments.putInt(ARGUMENT_EPISODE, newEpisode)
        (activity as MangaActivity).updateEpisode(newEpisode)

        reset()
    }
}