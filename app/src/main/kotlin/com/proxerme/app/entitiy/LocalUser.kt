package com.proxerme.app.entitiy

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
data class LocalUser(val username: String, val password: String?, val id: String,
                     val imageId: String, val loginToken: String)