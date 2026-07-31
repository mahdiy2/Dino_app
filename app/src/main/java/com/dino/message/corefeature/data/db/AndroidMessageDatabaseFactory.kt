package com.dino.message.corefeature.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private const val MESSAGE_DATABASE_NAME = "chat_db"

fun createMessageDatabaseBuilder(context: Context): RoomDatabase.Builder<MessageDatabase> =
    Room.databaseBuilder(
        context.applicationContext,
        MessageDatabase::class.java,
        MESSAGE_DATABASE_NAME,
    )

fun buildMessageDatabase(context: Context): MessageDatabase =
    createMessageDatabaseBuilder(context)
        .addMigrations(*MessageDatabase.ALL_MIGRATIONS)
        .build()
