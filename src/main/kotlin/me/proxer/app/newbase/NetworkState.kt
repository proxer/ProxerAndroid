package me.proxer.app.newbase

import me.proxer.app.util.ErrorUtils

/**
 * @author Ruben Gees
 */
sealed class NetworkState {
    object Idle : NetworkState()
    object Loading : NetworkState()
    class Error(val errorAction: ErrorUtils.ErrorAction) : NetworkState()
}
