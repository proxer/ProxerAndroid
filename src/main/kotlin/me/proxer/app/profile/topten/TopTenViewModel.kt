package me.proxer.app.profile.topten

import com.gojuno.koptional.Optional
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import me.proxer.app.base.BaseViewModel
import me.proxer.app.profile.topten.TopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entity.user.TopTenEntry
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class TopTenViewModel(
    private val userId: Optional<String>,
    private val username: Optional<String>
) : BaseViewModel<ZippedTopTenResult>() {

    override val dataSingle: Single<ZippedTopTenResult>
        get() {
            val includeHentai = preferenceHelper.isAgeRestrictedMediaAllowed && StorageHelper.isLoggedIn

            return Singles.zip(
                partialSingle(includeHentai, Category.ANIME),
                partialSingle(includeHentai, Category.MANGA),
                zipper = { animeEntries, mangaEntries ->
                    ZippedTopTenResult(animeEntries, mangaEntries)
                }
            )
        }

    private fun partialSingle(includeHentai: Boolean, category: Category) = api.user()
        .topTen(userId.toNullable(), username.toNullable())
        .includeHentai(includeHentai)
        .category(category)
        .buildSingle()

    data class ZippedTopTenResult(val animeEntries: List<TopTenEntry>, val mangaEntries: List<TopTenEntry>)
}
