package me.proxer.app

import io.mockk.mockk
import org.junit.Test
import org.koin.android.ext.koin.useAndroidContext
import org.koin.core.KoinApplication
import org.koin.test.KoinTest
import org.koin.test.check.checkModules

/**
 * @author Ruben Gees
 */
class MainModulesKtTest : KoinTest {

    @Test
    fun `koin modules`() {
        KoinApplication.create()
            .useAndroidContext(mockk())
            .loadModules(modules)
            .checkModules()
    }
}
