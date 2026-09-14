package com.dino.message.corefeature.data.db

internal data class SqlMigrationSpec(
    val startVersion: Int,
    val endVersion: Int,
    val statements: List<String>,
)

internal object MessageDatabaseMigrationSql {
    val MIGRATION_1_2 = SqlMigrationSpec(
        startVersion = 1,
        endVersion = 2,
        statements = listOf("ALTER TABLE messages ADD COLUMN chatId TEXT NOT NULL DEFAULT ''"),
    )

    val MIGRATION_2_3 = SqlMigrationSpec(
        startVersion = 2,
        endVersion = 3,
        statements = listOf(
            "ALTER TABLE messages ADD COLUMN messageType TEXT NOT NULL DEFAULT 'text'",
            "ALTER TABLE messages ADD COLUMN attachmentType TEXT",
            "ALTER TABLE messages ADD COLUMN attachmentUrl TEXT",
            "ALTER TABLE messages ADD COLUMN attachmentMimeType TEXT",
            "ALTER TABLE messages ADD COLUMN attachmentFileName TEXT",
            "ALTER TABLE messages ADD COLUMN attachmentSizeBytes INTEGER",
        ),
    )

    val MIGRATION_3_4 = SqlMigrationSpec(3, 4, listOf("ALTER TABLE messages ADD COLUMN messageId TEXT"))
    val MIGRATION_4_5 = SqlMigrationSpec(
        4,
        5,
        listOf("CREATE UNIQUE INDEX IF NOT EXISTS index_messages_messageId ON messages (messageId)"),
    )
    val MIGRATION_5_6 = SqlMigrationSpec(
        5,
        6,
        listOf(
            "ALTER TABLE messages ADD COLUMN replyToMessageId TEXT",
            "ALTER TABLE messages ADD COLUMN replyToSender TEXT",
            "ALTER TABLE messages ADD COLUMN replyPreview TEXT",
        ),
    )
    val MIGRATION_6_7 = SqlMigrationSpec(
        6,
        7,
        listOf(
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
            """.trimIndent(),
            "CREATE UNIQUE INDEX IF NOT EXISTS index_group_messages_messageId ON group_messages (messageId)",
        ),
    )
    val MIGRATION_7_8 = SqlMigrationSpec(
        7,
        8,
        listOf(
            "ALTER TABLE messages ADD COLUMN isSeen INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE group_messages ADD COLUMN isSeen INTEGER NOT NULL DEFAULT 0",
        ),
    )
    val MIGRATION_8_9 = SqlMigrationSpec(
        8,
        9,
        listOf(
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
            """.trimIndent(),
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_crash_logs_queue_eventId
            ON crash_logs_queue (eventId)
            """.trimIndent(),
            """
            CREATE INDEX IF NOT EXISTS index_crash_logs_queue_createdAtEpochMillis
            ON crash_logs_queue (createdAtEpochMillis)
            """.trimIndent(),
        ),
    )
    val MIGRATION_9_10 = SqlMigrationSpec(
        9,
        10,
        listOf(
            "CREATE INDEX IF NOT EXISTS index_messages_chatId ON messages (chatId)",
            "CREATE INDEX IF NOT EXISTS index_group_messages_groupId ON group_messages (groupId)",
        ),
    )
    val MIGRATION_10_11 = SqlMigrationSpec(
        10,
        11,
        listOf(
            "CREATE INDEX IF NOT EXISTS index_messages_chatId_timestamp_id ON messages (chatId, timestamp, id)",
            "CREATE INDEX IF NOT EXISTS index_group_messages_groupId_timestamp_id ON group_messages (groupId, timestamp, id)",
        ),
    )
    val MIGRATION_11_12 = SqlMigrationSpec(
        11,
        12,
        listOf(
            """
            CREATE TABLE IF NOT EXISTS socket_outbound_queue (
                queueId TEXT NOT NULL,
                event TEXT NOT NULL,
                ownerUsername TEXT NOT NULL,
                payloadType TEXT NOT NULL,
                payloadBody TEXT,
                dedupeKey TEXT,
                attempts INTEGER NOT NULL,
                nextAttemptAtMs INTEGER NOT NULL,
                createdAtMs INTEGER NOT NULL,
                PRIMARY KEY(queueId)
            )
            """.trimIndent(),
            "CREATE INDEX IF NOT EXISTS index_socket_outbound_queue_nextAttemptAtMs ON socket_outbound_queue (nextAttemptAtMs)",
            "CREATE UNIQUE INDEX IF NOT EXISTS index_socket_outbound_queue_dedupeKey ON socket_outbound_queue (dedupeKey)",
        ),
    )
    val MIGRATION_12_13 = SqlMigrationSpec(
        12,
        13,
        listOf(
            "ALTER TABLE messages ADD COLUMN reactionsJson TEXT",
            "ALTER TABLE group_messages ADD COLUMN reactionsJson TEXT",
        ),
    )
}
