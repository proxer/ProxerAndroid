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
class ErrorHandlerTest {

    @org.junit.Test
    fun getMessageForErrorCodeProxer() {
        assertEquals("test", ErrorHandler.getMessageForErrorCode(getTargetContext(),
                ProxerException(ProxerException.PROXER, "test")))
    }

    @org.junit.Test
    fun getMessageForErrorCodeIO() {
        assertEquals(getTargetContext().getString(R.string.error_io),
                ErrorHandler.getMessageForErrorCode(getTargetContext(),
                        ProxerException(ProxerException.IO)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeNetwork() {
        assertEquals(getTargetContext().getString(R.string.error_network),
                ErrorHandler.getMessageForErrorCode(getTargetContext(),
                        ProxerException(ProxerException.NETWORK)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeTimeout() {
        assertEquals(getTargetContext().getString(R.string.error_timeout),
                ErrorHandler.getMessageForErrorCode(getTargetContext(),
                        ProxerException(ProxerException.TIMEOUT)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeUnparseable() {
        assertEquals(getTargetContext().getString(R.string.error_unparseable),
                ErrorHandler.getMessageForErrorCode(getTargetContext(),
                        ProxerException(ProxerException.UNPARSEABLE)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeUnknown() {
        assertEquals(getTargetContext().getString(R.string.error_unknown),
                ErrorHandler.getMessageForErrorCode(getTargetContext(),
                        ProxerException(ProxerException.UNKNOWN)))
    }
}