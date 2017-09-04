package me.proxer.app.exception

/**
 * @author Ruben Gees
 */
class AppRequiredException(val name: String, val appPackage: String) : Exception()
