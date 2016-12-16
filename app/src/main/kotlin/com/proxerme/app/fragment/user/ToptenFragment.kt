package com.proxerme.app.fragment.user

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.user.ToptenAdapter
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.fragment.user.ToptenFragment.ToptenInput
import com.proxerme.app.fragment.user.ToptenFragment.ZippedToptenResult
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.task.framework.ListeningTask
import com.proxerme.app.task.framework.ZippedTask
import com.proxerme.app.util.DeviceUtils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.user.entitiy.ToptenEntry
import com.proxerme.library.connection.user.request.ToptenRequest
import com.proxerme.library.parameters.CategoryParameter.ANIME
import com.proxerme.library.parameters.CategoryParameter.MANGA

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ToptenFragment : SingleLoadingFragment<Pair<ToptenInput, ToptenInput>,
        ZippedToptenResult>() {

    companion object {
        private const val ARGUMENT_USER_ID = "user_id"
        private const val ARGUMENT_USER_NAME = "user_name"

        fun newInstance(userId: String? = null, userName: String? = null): ToptenFragment {
            if (userId.isNullOrBlank() && userName.isNullOrBlank()) {
                throw IllegalArgumentException("You must provide at least one of the arguments")
            }

            return ToptenFragment().apply {
                this.arguments = android.os.Bundle().apply {
                    this.putString(ToptenFragment.Companion.ARGUMENT_USER_ID, userId)
                    this.putString(ToptenFragment.Companion.ARGUMENT_USER_NAME, userName)
                }
            }
        }
    }

    override val section = Section.TOPTEN

    private var userId: String? = null
    private var username: String? = null

    private lateinit var animeAdapter: ToptenAdapter
    private lateinit var mangaAdapter: ToptenAdapter

    private val animeContainer: ViewGroup by bindView(com.proxerme.app.R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(com.proxerme.app.R.id.mangaContainer)
    private val animeList: RecyclerView by bindView(com.proxerme.app.R.id.animeList)
    private val mangaList: RecyclerView by bindView(com.proxerme.app.R.id.mangaList)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        userId = arguments.getString(ToptenFragment.Companion.ARGUMENT_USER_ID)
        username = arguments.getString(ToptenFragment.Companion.ARGUMENT_USER_NAME)

        animeAdapter = ToptenAdapter()
        animeAdapter.callback = object : ToptenAdapter.ToptenAdapterCallback() {
            override fun onItemClick(item: ToptenEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }

        mangaAdapter = ToptenAdapter()
        mangaAdapter.callback = object : ToptenAdapter.ToptenAdapterCallback() {
            override fun onItemClick(item: ToptenEntry) {
                MediaActivity.navigateTo(activity, item.id, item.name)
            }
        }
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?,
                              savedInstanceState: android.os.Bundle?): android.view.View {
        return inflater.inflate(com.proxerme.app.R.layout.fragment_topten, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animeList.isNestedScrollingEnabled = false
        animeList.layoutManager = GridLayoutManager(context,
                DeviceUtils.calculateSpanAmount(activity) + 1)
        animeList.adapter = animeAdapter
        mangaList.isNestedScrollingEnabled = false
        mangaList.layoutManager = GridLayoutManager(context,
                DeviceUtils.calculateSpanAmount(activity) + 1)
        mangaList.adapter = mangaAdapter
    }

    override fun onDestroyView() {
        animeList.adapter = null
        mangaList.adapter = null
        animeList.layoutManager = null
        mangaList.layoutManager = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        animeAdapter.removeCallback()
        mangaAdapter.removeCallback()

        super.onDestroy()
    }

    override fun present(data: ZippedToptenResult) {
        if (data.animeEntries.isEmpty() && data.mangaEntries.isEmpty()) {
            showError(getString(R.string.error_no_data_topten), null)
        } else {
            animeAdapter.replace(data.animeEntries)
            mangaAdapter.replace(data.mangaEntries)
        }

        if (animeAdapter.isEmpty()) {
            animeContainer.visibility = View.GONE
        } else {
            animeContainer.visibility = View.VISIBLE
        }

        if (mangaAdapter.isEmpty()) {
            mangaContainer.visibility = View.GONE
        } else {
            mangaContainer.visibility = View.VISIBLE
        }
    }

    override fun constructTask(): ListenableTask<Pair<ToptenInput, ToptenInput>,
            ZippedToptenResult> {
        return ListeningTask(ZippedTask<ToptenInput, ToptenInput, Array<ToptenEntry>,
                Array<ToptenEntry>, ZippedToptenResult>(
                ProxerLoadingTask({ ToptenRequest(userId, username, ANIME) }),
                ProxerLoadingTask({ ToptenRequest(userId, username, MANGA) }),
                zipFunction = ::ZippedToptenResult
        ))
    }

    override fun constructInput(): Pair<ToptenInput, ToptenInput> {
        return Pair(ToptenInput(userId, username), ToptenInput(userId, username))
    }

    class ToptenInput(val userId: String?, val username: String?)
    class ZippedToptenResult(val animeEntries: Array<ToptenEntry>,
                             val mangaEntries: Array<ToptenEntry>)
}