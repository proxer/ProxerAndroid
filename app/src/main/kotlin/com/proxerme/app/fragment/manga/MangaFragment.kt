package com.proxerme.app.fragment.manga

import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.activity.MangaActivity
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.manga.MangaAdapter
import com.proxerme.app.application.MainApplication
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.manga.entity.Chapter
import com.proxerme.library.connection.manga.request.ChapterRequest
import com.proxerme.library.connection.ucp.request.SetReminderRequest
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.jetbrains.anko.find
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
        private const val REMINDER_EPISODE_STATE = "reminder_episode_state"

        private const val DATE_PATTERN = "dd.MM.yyyy"

        private const val ICON_SIZE = 56
        private const val ICON_PADDING = 8

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

    private lateinit var mangaAdapter: MangaAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter
    private var chapter: Chapter? = null

    private var reminderEpisode: Int? = null
    private var reminderTask: ProxerCall? = null

    private val root: ViewGroup by bindView(R.id.root)

    private lateinit var header: ViewGroup
    private lateinit var footer: ViewGroup

    private lateinit var uploader: TextView
    private lateinit var scangroup: TextView
    private lateinit var date: TextView
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var reminderThis: Button
    private lateinit var reminderNext: Button
    private lateinit var scrollToTop: FloatingActionButton

    private val pages: RecyclerView by bindView(R.id.pages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            chapter = it.getParcelable(CHAPTER_STATE)
            reminderEpisode = it.getInt(REMINDER_EPISODE_STATE)

            if (reminderEpisode == 0) {
                reminderEpisode = null
            }
        }

        mangaAdapter = MangaAdapter(savedInstanceState)
        adapter = EasyHeaderFooterAdapter(mangaAdapter)

        synchronize(reminderEpisode)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.item_manga_header, container, false) as ViewGroup
        footer = inflater.inflate(R.layout.item_manga_footer, container, false) as ViewGroup

        uploader = header.find(R.id.uploader)
        scangroup = header.find(R.id.scangroup)
        date = header.find(R.id.date)
        previous = header.find(R.id.previous)
        next = header.find(R.id.next)
        reminderThis = header.find(R.id.reminderThis)
        reminderNext = header.find(R.id.reminderNext)
        scrollToTop = footer.find(R.id.scrollToTop)

        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN === View.VISIBLE) {
                    (activity as AppCompatActivity).supportActionBar?.show()
                } else {
                    (activity as AppCompatActivity).supportActionBar?.hide()
                }
            }
        }

        pages.isNestedScrollingEnabled = false
        pages.layoutManager = LinearLayoutManager(context)
        pages.adapter = adapter

        scrollToTop.setImageDrawable(IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_chevron_up)
                .sizeDp(ICON_SIZE)
                .paddingDp(ICON_PADDING)
                .colorRes(android.R.color.white))
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_manga, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fullscreen -> {
                if (activity.window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_FULLSCREEN === View.SYSTEM_UI_FLAG_FULLSCREEN) {
                    showSystemUI()
                } else {
                    hideSystemUI()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(REMINDER_EPISODE_STATE, reminderEpisode ?: 0)
        outState.putParcelable(CHAPTER_STATE, chapter)
        mangaAdapter.saveInstanceState(outState)
    }

    override fun save(result: Chapter) {
        chapter = result

        mangaAdapter.init(result.server, result.entryId, result.id)
        mangaAdapter.replace(result.pages)
    }

    override fun show() {
        if (chapter != null) {
            uploader.text = chapter!!.uploader

            if (chapter!!.scangroup == null) {
                scangroup.text = context.getString(R.string.fragment_manga_empty_scangroup)
            } else {
                scangroup.text = chapter!!.scangroup
            }

            uploader.setOnClickListener { view ->
                UserActivity.navigateTo(activity, chapter!!.uploaderId, chapter!!.uploader)
            }

            scangroup.setOnClickListener { view ->
                chapter!!.scangroup?.let {
                    Utils.viewLink(context, ProxerUrlHolder.getSubgroupUrl(it,
                            ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT).toString())
                }
            }

            date.text = DateTime(chapter!!.time * 1000).toString(DATE_PATTERN)

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
                reminderNext.visibility = View.GONE
            } else {
                next.visibility = View.VISIBLE
                next.text = getString(R.string.fragment_manga_next_chapter)
                next.setOnClickListener {
                    switchEpisode(episode + 1)
                }

                reminderNext.visibility = View.VISIBLE
                reminderNext.setOnClickListener {
                    synchronize(episode + 1)
                }
            }

            reminderThis.setOnClickListener {
                if (episode != reminderEpisode) {
                    synchronize(episode)
                }
            }

            scrollToTop.setOnClickListener {
                pages.scrollToPosition(0)
            }

            if (!adapter.hasHeader()) {
                adapter.setHeader(header)
            }

            if (!adapter.hasFooter()) {
                adapter.setFooter(footer)
            }
        } else {
            adapter.removeHeader()
            adapter.removeFooter()

            (header.parent as ViewGroup?)?.removeAllViews()
            (footer.parent as ViewGroup?)?.removeAllViews()
        }
    }

    override fun clear() {
        chapter = null

        adapter.removeHeader()
        adapter.removeFooter()

        (header.parent as ViewGroup?)?.removeAllViews()
        (footer.parent as ViewGroup?)?.removeAllViews()

        mangaAdapter.clear()
    }

    override fun constructLoadingRequest(): LoadingRequest<Chapter> {
        return LoadingRequest(ChapterRequest(id, episode, language))
    }

    private fun switchEpisode(newEpisode: Int) {
        arguments.putInt(ARGUMENT_EPISODE, newEpisode)
        (activity as MangaActivity).updateEpisode(newEpisode)

        reset()
    }

    @Synchronized
    private fun synchronize(episodeToSet: Int? = null) {
        if (episodeToSet == null) {
            reminderTask?.cancel()

            reminderTask = null
            reminderEpisode = null
        } else if (episodeToSet != reminderEpisode) {
            reminderTask?.cancel()

            reminderEpisode = episodeToSet
            reminderTask = MainApplication.proxerConnection.execute(SetReminderRequest(id,
                    reminderEpisode!!, language, CategoryParameter.MANGA),
                    {
                        reminderTask = null
                        reminderEpisode = null

                        Snackbar.make(root, R.string.fragment_set_reminder_success,
                                Snackbar.LENGTH_LONG).show()
                    },
                    {
                        reminderTask = null
                        reminderEpisode = null

                        Snackbar.make(root, R.string.fragment_set_reminder_error,
                                Snackbar.LENGTH_LONG).show()
                    })
        }
    }

    private fun showSystemUI() {
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
        } else {
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}