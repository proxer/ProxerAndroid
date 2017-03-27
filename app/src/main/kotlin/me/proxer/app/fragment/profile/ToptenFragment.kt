package me.proxer.app.fragment.profile

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.adapter.profile.ToptenAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.fragment.profile.ToptenFragment.ZippedTopTenResult
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.bindView
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.user.TopTenEntry
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class ToptenFragment : LoadingFragment<Pair<ProxerCall<List<TopTenEntry>>, ProxerCall<List<TopTenEntry>>>, ZippedTopTenResult>() {

    companion object {
        fun newInstance(): ToptenFragment {
            return ToptenFragment().apply {
                arguments = Bundle()
            }
        }
    }

    private val profileActivity
        get() = activity as me.proxer.app.activity.ProfileActivity

    private val userId: String?
        get() = profileActivity.userId
    private val username: String?
        get() = profileActivity.username

    private lateinit var animeAdapter: ToptenAdapter
    private lateinit var mangaAdapter: ToptenAdapter

    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeList: RecyclerView by bindView(R.id.animeList)
    private val mangaList: RecyclerView by bindView(R.id.mangaList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        animeAdapter = ToptenAdapter()
        animeAdapter.callback = object : ToptenAdapter.TopTenAdapterCallback {
            override fun onTopTenEntryClick(item: TopTenEntry) {
//                MediaActivity.navigateTo(activity, item.id, item.name, item.category)
            }
        }

        mangaAdapter = ToptenAdapter()
        mangaAdapter.callback = object : ToptenAdapter.TopTenAdapterCallback {
            override fun onTopTenEntryClick(item: TopTenEntry) {
//                MediaActivity.navigateTo(activity, item.id, item.name, item.category)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_topten, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = DeviceUtils.calculateSpanAmount(activity) + 1

        animeList.setHasFixedSize(true)
        animeList.isNestedScrollingEnabled = false
        animeList.layoutManager = GridLayoutManager(context, spanCount)
        animeList.adapter = animeAdapter

        mangaList.setHasFixedSize(true)
        mangaList.isNestedScrollingEnabled = false
        mangaList.layoutManager = GridLayoutManager(context, spanCount)
        mangaList.adapter = mangaAdapter
    }

    override fun onDestroyView() {
        animeAdapter.destroy()
        mangaAdapter.destroy()

        super.onDestroyView()
    }

    override fun onSuccess(result: ZippedTopTenResult) {
        animeAdapter.replace(result.animeEntries)
        mangaAdapter.replace(result.mangaEntries)

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

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (animeAdapter.isEmpty() && mangaAdapter.isEmpty()) {
            showError(R.string.error_no_data_top_ten, ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<TopTenEntry>>()
            .parallelWith(TaskBuilder.asyncProxerTask<List<TopTenEntry>>(), ::ZippedTopTenResult)
            .build()

    override fun constructInput() = api.user().topTen(userId, username)
            .category(Category.ANIME)
            .build() to api.user().topTen(userId, username)
            .category(Category.MANGA)
            .build()

    class ZippedTopTenResult(val animeEntries: List<TopTenEntry>,
                             val mangaEntries: List<TopTenEntry>)
}
