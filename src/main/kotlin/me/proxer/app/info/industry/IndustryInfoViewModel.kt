package me.proxer.app.info.industry

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Industry

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class IndustryInfoViewModel(private val industryId: String) : BaseContentViewModel<Industry>() {

    override val endpoint: Endpoint<Industry>
        get() = api.info().industry(industryId)
}
