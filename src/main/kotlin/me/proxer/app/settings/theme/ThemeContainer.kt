package me.proxer.app.settings.theme

/**
 * @author Ruben Gees
 */
data class ThemeContainer(val theme: Theme, val variant: ThemeVariant) {

    companion object {
        private const val DELIMITER = "_"

        fun fromPreferenceString(value: String): ThemeContainer {
            val split = value.split(DELIMITER)

            val theme = Theme.values().find { it.preferenceId == split.getOrNull(0) }
                ?: throw IllegalArgumentException("Invalid preference String $value")

            val variant = ThemeVariant.values().find { it.preferenceId == split.getOrNull(1) }
                ?: throw IllegalArgumentException("Invalid preference String $value")

            return ThemeContainer(theme, variant)
        }
    }

    fun toPreferenceString() = "${theme.preferenceId}$DELIMITER${variant.preferenceId}"
}
