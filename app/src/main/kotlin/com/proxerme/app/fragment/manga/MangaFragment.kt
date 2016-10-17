package com.proxerme.app.fragment.manga

import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
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

    private lateinit var adapter: MangaAdapter
    private var chapter: Chapter? = null

    private var reminderEpisode: Int? = null
    private var reminderTask: ProxerCall? = null

    private val root: ViewGroup by bindView(R.id.root)
    private val scrollContainer: NestedScrollView by bindView(R.id.scrollContainer)

    private val uploader: TextView by bindView(R.id.uploader)
    private val scangroup: TextView by bindView(R.id.scangroup)
    private val date: TextView by bindView(R.id.date)
    private val previous: Button by bindView(R.id.previous)
    private val next: Button by bindView(R.id.next)
    private val reminderThis: Button by bindView(R.id.reminderThis)
    private val reminderNext: Button by bindView(R.id.reminderNext)
    private val pages: RecyclerView by bindView(R.id.pages)
    private val scrollToTop: FloatingActionButton by bindView(R.id.scrollToTop)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            chapter = it.getParcelable(CHAPTER_STATE)
            reminderEpisode = it.getInt(REMINDER_EPISODE_STATE)

            if (reminderEpisode == 0) {
                reminderEpisode = null
            }
        }

        adapter = MangaAdapter(savedInstanceState)

        synchronize(reminderEpisode)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?,
                              savedInstanceState: android.os.Bundle?): android.view.View {
        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
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
                scrollContainer.smoothScrollTo(0, 0)
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