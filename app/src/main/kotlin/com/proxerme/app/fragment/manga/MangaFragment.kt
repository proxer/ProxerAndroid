package com.proxerme.app.fragment.manga

import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
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
import com.proxerme.app.util.bindView
import com.proxerme.app.view.MediaControlView
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

    private var reminderEpisode: Int? = null
    private var reminderTask: ProxerCall? = null

    private lateinit var header: MediaControlView
    private lateinit var footer: ViewGroup

    private lateinit var scrollToTop: FloatingActionButton

    private val root: ViewGroup by bindView(R.id.root)
    private val pages: RecyclerView by bindView(R.id.pages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            result = it.getParcelable(CHAPTER_STATE)
            reminderEpisode = it.getInt(REMINDER_EPISODE_STATE)

            if (reminderEpisode == 0) {
                reminderEpisode = null
            }
        }

        mangaAdapter = MangaAdapter()
        adapter = EasyHeaderFooterAdapter(mangaAdapter)

        synchronize(reminderEpisode)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.item_media_header, container, false) as MediaControlView
        footer = inflater.inflate(R.layout.item_manga_footer, container, false) as ViewGroup
        scrollToTop = footer.find(R.id.scrollToTop)

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun next() = context.getString(R.string.fragment_manga_next_chapter)
            override fun previous() = context.getString(R.string.fragment_manga_previous_chapter)
            override fun reminderThis() =
                    context.getString(R.string.fragment_manga_reminder_this_chapter)

            override fun reminderNext() =
                    context.getString(R.string.fragment_manga_reminder_next_chapter)
        }

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

        pages.layoutManager = LinearLayoutManager(context)
        pages.adapter = adapter

        header.onTranslatorGroupClickListener = {
            result?.scangroupId?.let {
                Utils.viewLink(context, ProxerUrlHolder.getSubgroupUrl(it,
                        ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT).toString())
            }
        }

        header.onUploaderClickListener = {
            result?.let {
                UserActivity.navigateTo(activity, it.uploaderId, it.uploader)
            }
        }

        header.onReminderClickListener = {
            if (it != reminderEpisode) {
                synchronize(it)
            }
        }

        header.onSwitchClickListener = {
            switchEpisode(it)
        }

        scrollToTop.setImageDrawable(IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_chevron_up)
                .sizeDp(ICON_SIZE)
                .paddingDp(ICON_PADDING)
                .colorRes(android.R.color.white))

        scrollToTop.setOnClickListener {
            pages.scrollToPosition(0)
        }
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
        outState.putParcelable(CHAPTER_STATE, result)
    }

    override fun showContent(result: Chapter) {
        header.setUploader(result.uploader)
        header.setTranslatorGroup(result.scangroup ?:
                context.getString(R.string.fragment_manga_empty_scangroup))
        header.setDate(DateTime(result.time * 1000))
        header.setEpisodeInfo(totalEpisodes, episode)

        adapter.setHeader(header)
        adapter.setFooter(footer)

        mangaAdapter.init(result.server, result.entryId, result.id)
        mangaAdapter.replace(result.pages)
    }

    override fun clear() {
        adapter.removeHeader()
        adapter.removeFooter()

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