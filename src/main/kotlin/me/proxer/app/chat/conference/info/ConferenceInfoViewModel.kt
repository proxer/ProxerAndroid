package me.proxer.app.chat.conference.info

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.messenger.ConferenceInfo

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ConferenceInfoViewModel(private val conferenceId: String) : BaseContentViewModel<ConferenceInfo>() {

    override val endpoint: Endpoint<ConferenceInfo>
        get() = api.messenger().conferenceInfo(conferenceId)
}
