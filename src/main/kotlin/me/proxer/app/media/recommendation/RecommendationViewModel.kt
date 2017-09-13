package me.proxer.app.media.recommendation

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Recommendation

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class RecommendationViewModel(private val entryId: String) : BaseContentViewModel<List<Recommendation>>() {

    override val endpoint: Endpoint<List<Recommendation>>
        get() = api.info().recommendations(entryId)
}
