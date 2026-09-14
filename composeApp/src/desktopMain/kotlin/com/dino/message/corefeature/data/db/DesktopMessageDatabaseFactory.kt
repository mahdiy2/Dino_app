package com.dino.message.corefeature.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createMessageDatabaseBuilder(databasePath: String): RoomDatabase.Builder<MessageDatabase> =
    Room.databaseBuilder<MessageDatabase>(
        name = databasePath,
        factory = MessageDatabaseConstructor::initialize,
    )

fun buildMessageDatabase(databasePath: String): MessageDatabase = MessageDatabase.configure(
    createMessageDatabaseBuilder(databasePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO),
)
