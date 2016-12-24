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
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.adapter.manga.MangaAdapter
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.fragment.manga.MangaFragment.MangaInput
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.Validators
import com.proxerme.app.util.ViewUtils
import com.proxerme.app.util.bindView
import com.proxerme.app.view.MediaControlView
import com.proxerme.library.connection.info.request.SetUserInfoRequest
import com.proxerme.library.connection.manga.entity.Chapter
import com.proxerme.library.connection.manga.request.ChapterRequest
import com.proxerme.library.connection.ucp.request.SetReminderRequest
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.ViewStateParameter
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.jetbrains.anko.find
import org.joda.time.DateTime

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MangaFragment : SingleLoadingFragment<MangaInput, Chapter>() {

    companion object {

        private const val ARGUMENT_ID = "id"
        private const val ARGUMENT_EPISODE = "episode"
        private const val ARGUMENT_TOTAL_EPISODES = "total_episodes"
        private const val ARGUMENT_LANGUAGE = "language"

        private const val SCROLL_TO_TOP_ICON_SIZE = 56
        private const val SCROLL_TO_TOP_ICON_PADDING = 8

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

    private val reminderSuccess = { nothing: Void? ->
        Snackbar.make(root, R.string.fragment_set_user_info_success, Snackbar.LENGTH_LONG).show()
    }

    private val reminderException = { exception: Exception ->
        val action = ErrorUtils.handle(activity as AppCompatActivity, exception)

        ViewUtils.makeMultilineSnackbar(root,
                getString(R.string.fragment_set_user_info_error, action.message),
                Snackbar.LENGTH_LONG).setAction(action.buttonMessage, action.buttonAction).show()
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

    private var reminderTask = constructReminderTask()

    private lateinit var header: MediaControlView
    private lateinit var footer: ViewGroup

    private lateinit var scrollToTop: FloatingActionButton

    private val root: ViewGroup by bindView(R.id.root)
    private val pages: RecyclerView by bindView(R.id.pages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mangaAdapter = MangaAdapter()
        adapter = EasyHeaderFooterAdapter(mangaAdapter)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.item_media_header, container, false) as MediaControlView
        footer = inflater.inflate(R.layout.item_manga_footer, container, false) as ViewGroup
        scrollToTop = footer.find(R.id.scrollToTop)

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun finish() = context.getString(R.string.finished_media)
            override fun next() = context.getString(R.string.fragment_manga_next_chapter)
            override fun previous() = context.getString(R.string.fragment_manga_previous_chapter)
            override fun reminderThis() =
                    context.getString(R.string.fragment_manga_reminder_this_chapter)

            override fun reminderNext() =
                    context.getString(R.string.fragment_manga_reminder_next_chapter)
        }

        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
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
            showPage(ProxerUrlHolder.getSubgroupUrl(it.id,
                    ProxerUrlHolder.DEVICE_QUERY_PARAMETER_DEFAULT))
        }

        header.onUploaderClickListener = {
            ProfileActivity.navigateTo(activity, it.id, it.name)
        }

        header.onReminderClickListener = {
            reminderTask.execute(ReminderInput(id, it, language, false))
        }

        header.onFinishClickListener = {
            reminderTask.execute(ReminderInput(id, it, language, true))
        }

        header.onSwitchClickListener = {
            switchEpisode(it)
        }

        scrollToTop.setImageDrawable(IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_chevron_up)
                .sizeDp(SCROLL_TO_TOP_ICON_SIZE)
                .paddingDp(SCROLL_TO_TOP_ICON_PADDING)
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

    override fun onDestroyView() {
        adapter.removeHeader()
        adapter.removeFooter()
        pages.adapter = null
        pages.layoutManager = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        reminderTask.destroy()

        super.onDestroy()
    }

    override fun present(data: Chapter) {
        header.setDate(DateTime(data.time * 1000))
        header.setEpisodeInfo(totalEpisodes, episode)
        header.setUploader(MediaControlView.Uploader(data.uploaderId, data.uploader))
        header.setTranslatorGroup(when (data.scangroupId == null || data.scangroup == null) {
            true -> null
            else -> MediaControlView.TranslatorGroup(data.scangroupId!!, data.scangroup!!)
        })

        data.scangroupId?.let { id ->
            data.scangroup?.let { name ->
                header.setTranslatorGroup(MediaControlView.TranslatorGroup(id, name))
            }
        }

        adapter.setHeader(header)
        adapter.setFooter(footer)

        mangaAdapter.init(data.server, data.entryId, data.id)
        mangaAdapter.replace(data.pages)
    }

    override fun clear() {
        adapter.removeHeader()
        adapter.removeFooter()

        mangaAdapter.clear()

        super.clear()
    }

    override fun constructTask(): ListenableTask<MangaInput, Chapter> {
        return ProxerLoadingTask({ ChapterRequest(it.id, it.episode, it.language) })
    }

    private fun constructReminderTask(): Task<ReminderInput, Void?> {
        return ValidatingTask(ProxerLoadingTask({
            if (it.isFinished) {
                SetUserInfoRequest(it.id, ViewStateParameter.FINISHED)
            } else {
                SetReminderRequest(it.id, it.episode, it.language, CategoryParameter.MANGA)
            }
        }), { Validators.validateLogin() }, reminderSuccess, reminderException)
    }

    override fun constructInput(): MangaInput {
        return MangaInput(id, episode, language)
    }

    private fun switchEpisode(newEpisode: Int) {
        arguments.putInt(ARGUMENT_EPISODE, newEpisode)
        (activity as MangaActivity).updateEpisode(newEpisode)

        reset()
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

    open class MangaInput(val id: String, val episode: Int, val language: String)
    class ReminderInput(id: String, episode: Int, language: String, val isFinished: Boolean) :
            MangaInput(id, episode, language)
}