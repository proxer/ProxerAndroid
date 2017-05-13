package me.proxer.app.fragment.profile

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.profile.TopTenAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.fragment.profile.TopTenFragment.ZippedTopTenResult
import me.proxer.app.task.proxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.bindView
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.user.TopTenEntry
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class TopTenFragment : LoadingFragment<Pair<ProxerCall<List<TopTenEntry>>, ProxerCall<List<TopTenEntry>>>,
        ZippedTopTenResult>() {

    companion object {
        fun newInstance(): TopTenFragment {
            return TopTenFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    private val profileActivity
        get() = activity as me.proxer.app.activity.ProfileActivity

    private val userId: String?
        get() = profileActivity.userId

    private val username: String?
        get() = profileActivity.username

    private val animeAdapter by lazy { TopTenAdapter() }
    private val mangaAdapter by lazy { TopTenAdapter() }

    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeList: RecyclerView by bindView(R.id.animeList)
    private val mangaList: RecyclerView by bindView(R.id.mangaList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_top_ten, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spanCount = DeviceUtils.calculateSpanAmount(activity) + 1

        animeAdapter.callback = TopTenCallback()
        mangaAdapter.callback = TopTenCallback()

        animeList.setHasFixedSize(true)
        animeList.isNestedScrollingEnabled = false
        animeList.layoutManager = GridLayoutManager(context, spanCount)
        animeList.adapter = animeAdapter

        mangaList.setHasFixedSize(true)
        mangaList.isNestedScrollingEnabled = false
        mangaList.layoutManager = GridLayoutManager(context, spanCount)
        mangaList.adapter = mangaAdapter
    }

    override fun onSuccess(result: ZippedTopTenResult) {
        animeAdapter.replace(result.animeEntries)
        mangaAdapter.replace(result.mangaEntries)

        when (animeAdapter.isEmpty()) {
            true -> animeContainer.visibility = View.GONE
            false -> animeContainer.visibility = View.VISIBLE
        }

        when (mangaAdapter.isEmpty()) {
            true -> mangaContainer.visibility = View.GONE
            false -> mangaContainer.visibility = View.VISIBLE
        }

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (animeAdapter.isEmpty() && mangaAdapter.isEmpty()) {
            showError(R.string.error_no_data_top_ten, ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    override fun constructTask() = TaskBuilder.proxerTask<List<TopTenEntry>>()
            .parallelWith(TaskBuilder.proxerTask<List<TopTenEntry>>(), ::ZippedTopTenResult)
            .build()

    override fun constructInput() = api.user().topTen(userId, username)
            .category(Category.ANIME)
            .build() to api.user().topTen(userId, username)
            .category(Category.MANGA)
            .build()

    class ZippedTopTenResult(val animeEntries: List<TopTenEntry>, val mangaEntries: List<TopTenEntry>)

    private inner class TopTenCallback : TopTenAdapter.TopTenAdapterCallback {
        override fun onTopTenEntryClick(view: View, item: TopTenEntry) {
            val imageView = view.find<ImageView>(R.id.image)

            MediaActivity.navigateTo(activity, item.id, item.name, item.category,
                    if (imageView.drawable != null) imageView else null)
        }
    }
}
