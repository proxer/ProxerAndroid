package app

import me.proxer.app.modules
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.checkModules

/**
 * @author Ruben Gees
 */
class MainModulesKtTest : KoinTest {

    @Test
    fun `koin modules`() {
        checkModules(modules)
    }
}
