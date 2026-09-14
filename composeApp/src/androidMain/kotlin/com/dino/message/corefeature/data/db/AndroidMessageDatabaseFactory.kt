package com.dino.message.corefeature.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun createMessageDatabaseBuilder(context: Context): RoomDatabase.Builder<MessageDatabase> =
    Room.databaseBuilder<MessageDatabase>(
        context.applicationContext,
        MESSAGE_DATABASE_NAME,
        MessageDatabaseConstructor::initialize,
    )

fun buildMessageDatabase(context: Context): MessageDatabase =
    MessageDatabase.configure(createMessageDatabaseBuilder(context))
