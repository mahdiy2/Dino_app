package com.dino.message.corefeature.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dino.message.chatfeature.data.db.MessageDao
import com.dino.message.chatfeature.data.local.MessageLocalEntity
import com.dino.message.corefeature.data.socket.SocketOutboundQueueDao
import com.dino.message.corefeature.data.socket.SocketOutboundQueueEntity
import com.dino.message.crashfeature.data.local.CrashLogDao
import com.dino.message.crashfeature.data.local.CrashLogEntity
import com.dino.message.groupfeature.data.db.GroupMessageDao
import com.dino.message.groupfeature.data.local.GroupMessageLocalEntity

@Database(
    entities = [
        MessageLocalEntity::class,
        GroupMessageLocalEntity::class,
        CrashLogEntity::class,
        SocketOutboundQueueEntity::class,
    ],
    version = 13,
)
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

        private fun roomMigration(spec: SqlMigrationSpec) = object : Migration(spec.startVersion, spec.endVersion) {
            override fun migrate(db: SupportSQLiteDatabase) {
                spec.statements.forEach(db::execSQL)
            }
        }
    }
}
