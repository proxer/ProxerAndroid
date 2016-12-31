package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.activity.MediaActivity
import com.proxerme.app.adapter.ucp.UcpToptenAdapter
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.fragment.ucp.UcpToptenFragment.ZippedUcpToptenResult
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.MappedTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.util.*
import com.proxerme.library.connection.ucp.entitiy.UcpToptenEntry
import com.proxerme.library.connection.ucp.request.DeleteFavoriteRequest
import com.proxerme.library.connection.ucp.request.UcpToptenRequest
import com.proxerme.library.parameters.CategoryParameter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UcpToptenFragment : SingleLoadingFragment<Unit, ZippedUcpToptenResult>() {

    companion object {
        fun newInstance(): UcpToptenFragment {
            return UcpToptenFragment()
        }
    }

    private val removalSuccess = { nothing: Void? ->
        val newResult = ZippedUcpToptenResult(
                animeAdapter.items.filterNot { it == itemToRemove }.toTypedArray(),
                mangaAdapter.items.filterNot { it == itemToRemove }.toTypedArray()
        )

        cache.mutate { newResult }

        if (view != null) {
            present(newResult)
        }
    }

    private val removalException = { exception: Exception ->
        itemToRemove = null

        if (view != null) {
            val action = ErrorUtils.handle(activity as MainActivity, exception)

            ViewUtils.makeMultilineSnackbar(root,
                    context.getString(R.string.error_topten_removal, action.message),
                    Snackbar.LENGTH_LONG).setAction(action.buttonMessage, action.buttonAction)
                    .show()
        }
    }

    override val section = Section.TOPTEN
    override val isLoginRequired = true

    private val removalTask = constructRemovalTask()
    private var itemToRemove: UcpToptenEntry? = null

    private fun constructRemovalTask(): Task<UcpToptenEntry, Void?> {
        return ValidatingTask(ProxerLoadingTask({
            DeleteFavoriteRequest(it.id)
        }), { Validators.validateLogin() }, removalSuccess, removalException)
    }

    private lateinit var animeAdapter: UcpToptenAdapter
    private lateinit var mangaAdapter: UcpToptenAdapter

    private val animeContainer: ViewGroup by bindView(R.id.animeContainer)
    private val mangaContainer: ViewGroup by bindView(R.id.mangaContainer)
    private val animeList: RecyclerView by bindView(R.id.animeList)
    private val mangaList: RecyclerView by bindView(R.id.mangaList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        animeAdapter = UcpToptenAdapter()
        animeAdapter.callback = object : UcpToptenAdapter.UcpToptenAdapterCallback() {
            override fun onItemClick(item: UcpToptenEntry) {
                MediaActivity.navigateTo(activity, item.entryId, item.name)
            }

            override fun onRemoveClick(item: UcpToptenEntry) {
                itemToRemove = item

                removalTask.execute(item)
            }
        }

        mangaAdapter = UcpToptenAdapter()
        mangaAdapter.callback = object : UcpToptenAdapter.UcpToptenAdapterCallback() {
            override fun onItemClick(item: UcpToptenEntry) {
                MediaActivity.navigateTo(activity, item.entryId, item.name)
            }

            override fun onRemoveClick(item: UcpToptenEntry) {
                itemToRemove = item

                removalTask.execute(item)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_topten, container, false)
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
        removalTask.destroy()
        animeAdapter.removeCallback()
        mangaAdapter.removeCallback()

        super.onDestroy()
    }

    override fun present(data: ZippedUcpToptenResult) {
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

    override fun constructTask(): Task<Unit, ZippedUcpToptenResult> {
        return MappedTask(ProxerLoadingTask({ UcpToptenRequest() }), { it: Array<UcpToptenEntry> ->
            ZippedUcpToptenResult(
                    it.filter { it.category == CategoryParameter.ANIME }.toTypedArray(),
                    it.filter { it.category == CategoryParameter.MANGA }.toTypedArray()
            )
        })
    }

    override fun constructInput() {}

    class ZippedUcpToptenResult(val animeEntries: Array<UcpToptenEntry>,
                                val mangaEntries: Array<UcpToptenEntry>)
}