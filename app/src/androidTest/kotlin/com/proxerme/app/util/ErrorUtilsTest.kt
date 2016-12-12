package com.proxerme.app.util

import android.support.test.InstrumentationRegistry.getTargetContext
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.proxerme.app.R
import com.proxerme.library.connection.ProxerException
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

/**
 * TODO: Describe class

 * @author Ruben Gees
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ErrorUtilsTest {

    @org.junit.Test
    fun getMessageForErrorCodeProxer() {
        assertEquals("test", ErrorUtils.getMessageForErrorCode(getTargetContext(),
                ProxerException(ProxerException.PROXER, "test")))
    }

    @org.junit.Test
    fun getMessageForErrorCodeNetwork() {
        assertEquals(getTargetContext().getString(R.string.error_network),
                ErrorUtils.getMessageForErrorCode(getTargetContext(),
                        ProxerException(ProxerException.NETWORK)))
    }
}