package me.proxer.app.manga.decoder

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class FallbackManager {

    // TODO: Change DecoderFactory#make function to take key parameter.
    var nextKey: String = ""

    protected val fallbackMap = mutableMapOf<String, FallbackStage>()

    fun nextFallbackStage(key: String) = when (fallbackMap[key] ?: FallbackStage.NORMAL) {
        FallbackStage.NORMAL -> {
            fallbackMap[key] = FallbackStage.RAPID

            true
        }
        FallbackStage.RAPID -> {
            fallbackMap[key] = FallbackStage.NATIVE

            true
        }
        FallbackStage.NATIVE -> false
    }

    protected enum class FallbackStage {
        NORMAL, RAPID, NATIVE
    }
}
