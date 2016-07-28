package com.proxerme.app.util

import android.support.test.filters.SmallTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.proxerme.app.R
import com.proxerme.app.activity.DashboardActivity
import com.proxerme.library.connection.ProxerException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * TODO: Describe class

 * @author Ruben Gees
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ErrorHandlerTest {

    @Rule @JvmField
    var activityTestRule = ActivityTestRule(DashboardActivity::class.java)

    @org.junit.Test
    fun getMessageForErrorCodeProxer() {
        assertEquals("test", ErrorHandler.getMessageForErrorCode(activityTestRule.activity,
                ProxerException(ProxerException.PROXER, "test")))
    }

    @org.junit.Test
    fun getMessageForErrorCodeIO() {
        assertEquals(activityTestRule.activity.getString(R.string.error_io),
                ErrorHandler.getMessageForErrorCode(activityTestRule.activity,
                        ProxerException(ProxerException.IO)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeNetwork() {
        assertEquals(activityTestRule.activity.getString(R.string.error_network),
                ErrorHandler.getMessageForErrorCode(activityTestRule.activity,
                        ProxerException(ProxerException.NETWORK)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeTimeout() {
        assertEquals(activityTestRule.activity.getString(R.string.error_timeout),
                ErrorHandler.getMessageForErrorCode(activityTestRule.activity,
                        ProxerException(ProxerException.TIMEOUT)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeUnparseable() {
        assertEquals(activityTestRule.activity.getString(R.string.error_unparseable),
                ErrorHandler.getMessageForErrorCode(activityTestRule.activity,
                        ProxerException(ProxerException.UNPARSEABLE)))
    }

    @org.junit.Test
    fun getMessageForErrorCodeUnknown() {
        assertEquals(activityTestRule.activity.getString(R.string.error_unknown),
                ErrorHandler.getMessageForErrorCode(activityTestRule.activity,
                        ProxerException(ProxerException.UNKNOWN)))
    }
}