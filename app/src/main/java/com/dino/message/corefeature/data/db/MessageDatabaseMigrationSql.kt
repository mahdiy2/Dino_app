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

    val MIGRATION_3_4 = SqlMigrationSpec(
        startVersion = 3,
        endVersion = 4,
        statements = listOf("ALTER TABLE messages ADD COLUMN messageId TEXT"),
    )

    val MIGRATION_4_5 = SqlMigrationSpec(
        startVersion = 4,
        endVersion = 5,
        statements = listOf("CREATE UNIQUE INDEX IF NOT EXISTS index_messages_messageId ON messages (messageId)"),
    )

    val MIGRATION_5_6 = SqlMigrationSpec(
        startVersion = 5,
        endVersion = 6,
        statements = listOf(
            "ALTER TABLE messages ADD COLUMN replyToMessageId TEXT",
            "ALTER TABLE messages ADD COLUMN replyToSender TEXT",
            "ALTER TABLE messages ADD COLUMN replyPreview TEXT",
        ),
    )

    val MIGRATION_6_7 = SqlMigrationSpec(
        startVersion = 6,
        endVersion = 7,
        statements = listOf(
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
        startVersion = 7,
        endVersion = 8,
        statements = listOf(
            "ALTER TABLE messages ADD COLUMN isSeen INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE group_messages ADD COLUMN isSeen INTEGER NOT NULL DEFAULT 0",
        ),
    )

    val MIGRATION_8_9 = SqlMigrationSpec(
        startVersion = 8,
        endVersion = 9,
        statements = listOf(
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
        startVersion = 9,
        endVersion = 10,
        statements = listOf(
            """
            CREATE INDEX IF NOT EXISTS index_messages_chatId
            ON messages (chatId)
            """.trimIndent(),
            """
            CREATE INDEX IF NOT EXISTS index_group_messages_groupId
            ON group_messages (groupId)
            """.trimIndent(),
        ),
    )

    val MIGRATION_10_11 = SqlMigrationSpec(
        startVersion = 10,
        endVersion = 11,
        statements = listOf(
            """
            CREATE INDEX IF NOT EXISTS index_messages_chatId_timestamp_id
            ON messages (chatId, timestamp, id)
            """.trimIndent(),
            """
            CREATE INDEX IF NOT EXISTS index_group_messages_groupId_timestamp_id
            ON group_messages (groupId, timestamp, id)
            """.trimIndent(),
        ),
    )

    val MIGRATION_11_12 = SqlMigrationSpec(
        startVersion = 11,
        endVersion = 12,
        statements = listOf(
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
            """
            CREATE INDEX IF NOT EXISTS index_socket_outbound_queue_nextAttemptAtMs
            ON socket_outbound_queue (nextAttemptAtMs)
            """.trimIndent(),
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_socket_outbound_queue_dedupeKey
            ON socket_outbound_queue (dedupeKey)
            """.trimIndent(),
        ),
    )

    val MIGRATION_12_13 = SqlMigrationSpec(
        startVersion = 12,
        endVersion = 13,
        statements = listOf(
            "ALTER TABLE messages ADD COLUMN reactionsJson TEXT",
            "ALTER TABLE group_messages ADD COLUMN reactionsJson TEXT",
        ),
    )
}
