package com.dino.message.corefeature.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.dino.message.chatfeature.data.db.MessageDao
import com.dino.message.chatfeature.data.local.MessageLocalEntity
import com.dino.message.corefeature.data.socket.SocketOutboundQueueDao
import com.dino.message.corefeature.data.socket.SocketOutboundQueueEntity
import com.dino.message.crashfeature.data.local.CrashLogDao
import com.dino.message.crashfeature.data.local.CrashLogEntity
import com.dino.message.groupfeature.data.db.GroupMessageDao
import com.dino.message.groupfeature.data.local.GroupMessageLocalEntity

const val MESSAGE_DATABASE_NAME = "chat_db"
const val MESSAGE_DATABASE_VERSION = 13

@Database(
    entities = [
        MessageLocalEntity::class,
        GroupMessageLocalEntity::class,
        CrashLogEntity::class,
        SocketOutboundQueueEntity::class,
    ],
    version = MESSAGE_DATABASE_VERSION,
)
@ConstructedBy(MessageDatabaseConstructor::class)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun crashLogDao(): CrashLogDao
    abstract fun socketOutboundQueueDao(): SocketOutboundQueueDao

    companion object {
        val MIGRATION_1_2 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_1_2)
        val MIGRATION_2_3 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_2_3)
        val MIGRATION_3_4 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_3_4)
        val MIGRATION_4_5 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_4_5)
        val MIGRATION_5_6 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_5_6)
        val MIGRATION_6_7 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_6_7)
        val MIGRATION_7_8 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_7_8)
        val MIGRATION_8_9 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_8_9)
        val MIGRATION_9_10 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_9_10)
        val MIGRATION_10_11 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_10_11)
        val MIGRATION_11_12 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_11_12)
        val MIGRATION_12_13 = roomMigration(MessageDatabaseMigrationSql.MIGRATION_12_13)

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
        )

        fun configure(builder: Builder<MessageDatabase>): MessageDatabase = builder
            .addMigrations(*ALL_MIGRATIONS)
            .build()

        private fun roomMigration(spec: SqlMigrationSpec) =
            object : Migration(spec.startVersion, spec.endVersion) {
                override fun migrate(connection: SQLiteConnection) {
                    spec.statements.forEach(connection::execSQL)
                }
            }
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MessageDatabaseConstructor : RoomDatabaseConstructor<MessageDatabase> {
    override fun initialize(): MessageDatabase
}
