package me.proxer.app

import android.content.Context
import io.mockk.mockk
import org.junit.Test
import org.koin.dsl.module.module
import org.koin.test.KoinTest
import org.koin.test.checkModules

/**
 * @author Ruben Gees
 */
class MainModulesKtTest : KoinTest {

    @Test
    fun `koin modules`() {
        val contextMockModule = module {
            single { mockk<Context>() }
        }

        checkModules(modules + contextMockModule)
    }
}
