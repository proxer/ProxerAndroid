package me.proxer.app

import io.mockk.mockk
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.koin.test.check.checkModules

/**
 * @author Ruben Gees
 */
class MainModulesKtTest : KoinTest {

    @Test
    fun `koin modules`() {
        val koinApplication = koinApplication {
            androidContext(mockk())

            modules(koinModules)
        }

        koinApplication.checkModules()
    }
}
