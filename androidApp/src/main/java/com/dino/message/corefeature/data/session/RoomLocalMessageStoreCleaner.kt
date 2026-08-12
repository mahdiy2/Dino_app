package com.dino.message.corefeature.data.session

import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.corefeature.domain.session.LocalMessageStoreCleaner

class RoomLocalMessageStoreCleaner(
    private val messageDatabase: MessageDatabase,
) : LocalMessageStoreCleaner {
    override fun clearAllTables() {
        messageDatabase.clearAllTables()
    }
}
