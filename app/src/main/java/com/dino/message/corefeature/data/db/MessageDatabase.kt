package com.dino.message.corefeature.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dino.message.chatfeature.data.db.MessageDao
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.crashfeature.data.local.CrashLogDao
import com.dino.message.crashfeature.data.local.CrashLogEntity
import com.dino.message.groupfeature.data.db.GroupMessageDao
import com.dino.message.groupfeature.domain.model.GroupMessageEntity

@Database(
    entities = [
        MessageEntity::class,
        GroupMessageEntity::class,
        CrashLogEntity::class,
    ],
    version = 9,
)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun crashLogDao(): CrashLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN chatId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN messageType TEXT NOT NULL DEFAULT 'text'")
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentType TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentUrl TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentMimeType TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentFileName TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentSizeBytes INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN messageId TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_messages_messageId ON messages (messageId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToMessageId TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToSender TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyPreview TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS group_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        messageId TEXT NOT NULL,
                        groupId INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        messageType TEXT NOT NULL DEFAULT 'text',
                        attachmentType TEXT,
                        attachmentUrl TEXT,
                        attachmentMimeType TEXT,
                        attachmentFileName TEXT,
                        attachmentSizeBytes INTEGER,
                        replyToMessageId TEXT,
                        replyToSender TEXT,
                        replyPreview TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_messages_messageId ON group_messages (messageId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN isSeen INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE group_messages ADD COLUMN isSeen INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS crash_logs_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventId TEXT NOT NULL,
                        appVersion TEXT NOT NULL,
                        appVersionCode INTEGER NOT NULL,
                        threadName TEXT NOT NULL,
                        exceptionType TEXT NOT NULL,
                        exceptionMessage TEXT,
                        stackTrace TEXT NOT NULL,
                        occurredAtEpochMillis INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        deviceModel TEXT NOT NULL,
                        deviceManufacturer TEXT NOT NULL,
                        osVersion TEXT NOT NULL,
                        sdkInt INTEGER NOT NULL,
                        buildType TEXT NOT NULL,
                        username TEXT,
                        sendAttempts INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_crash_logs_queue_eventId
                    ON crash_logs_queue (eventId)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_crash_logs_queue_createdAtEpochMillis
                    ON crash_logs_queue (createdAtEpochMillis)
                    """.trimIndent()
                )
            }
        }
    }
}
