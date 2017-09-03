package me.proxer.app.profile.topten

import android.app.Application
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.base.BaseViewModel
import me.proxer.app.profile.topten.TopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entity.user.TopTenEntry
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class TopTenViewModel(application: Application) : BaseViewModel<ZippedTopTenResult>(application) {

    override val dataSingle: Single<ZippedTopTenResult>
        get() {
            val includeHentai = PreferenceHelper.isAgeRestrictedMediaAllowed(globalContext) && StorageHelper.user != null

            return Singles.zip(
                    partialSingle(includeHentai, Category.ANIME),
                    partialSingle(includeHentai, Category.MANGA),
                    { animeEntries, mangaEntries ->
                        ZippedTopTenResult(animeEntries, mangaEntries)
                    }
            )
        }

    var userId: String? = null
    var username: String? = null

    private fun partialSingle(includeHentai: Boolean, category: Category) = api.user().topTen(userId, username)
            .includeHentai(includeHentai)
            .category(category)
            .buildSingle()

    class ZippedTopTenResult(val animeEntries: List<TopTenEntry>, val mangaEntries: List<TopTenEntry>)
}
