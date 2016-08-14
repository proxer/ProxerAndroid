package com.proxerme.app.data

import com.proxerme.library.connection.messenger.entity.Message
import org.jetbrains.anko.db.RowParser

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MessageParser : RowParser<Message> {
    override fun parseRow(columns: Array<Any?>): Message {
        return Message(columns[0].toString(), columns[1].toString(), columns[2].toString(),
                columns[3].toString(), columns[4].toString(), columns[5].toString(),
                columns[6] as Long, columns[7].toString())
    }
}