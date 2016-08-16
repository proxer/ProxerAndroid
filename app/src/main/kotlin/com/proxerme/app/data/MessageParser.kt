package com.proxerme.app.data

import com.proxerme.app.entitiy.LocalMessage
import org.jetbrains.anko.db.RowParser

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MessageParser : RowParser<LocalMessage> {
    override fun parseRow(columns: Array<Any?>): LocalMessage {
        return LocalMessage(columns[0] as Long, columns[1].toString(), columns[2].toString(),
                columns[3].toString(), columns[4].toString(), columns[5].toString(),
                columns[6].toString(), columns[7] as Long, columns[8].toString())
    }
}