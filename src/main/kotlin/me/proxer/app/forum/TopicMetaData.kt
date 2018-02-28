package me.proxer.app.forum

import java.util.*

/**
 * @author Ruben Gees
 */
data class TopicMetaData(
        val categoryId: String,
        val categoryName: String,
        val firstPostDate: Date,
        val lastPostDate: Date,
        val hits: Int,
        val isLocked: Boolean,
        val postAmount: Int,
        val subject: String
)
