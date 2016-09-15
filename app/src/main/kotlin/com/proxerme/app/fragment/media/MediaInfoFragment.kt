package com.proxerme.app.fragment.media

import android.os.Bundle
import com.proxerme.app.fragment.framework.MainFragment
import com.proxerme.app.manager.SectionManager.Section

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaInfoFragment : MainFragment() {

    companion object {
        private const val ARGUMENT_ID = "id"

        fun newInstance(id: String): MediaInfoFragment {
            return MediaInfoFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.MEDIA_INFO

}