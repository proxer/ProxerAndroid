package me.proxer.app.media.recommendation

import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Recommendation

/**
 * @author Ruben Gees
 */
class RecommendationViewModel(private val entryId: String) : BaseContentViewModel<List<Recommendation>>() {

    override val endpoint: Endpoint<List<Recommendation>>
        get() = api.info().recommendations(entryId)
}
