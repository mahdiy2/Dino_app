package com.dino.message.corefeature.data.db

 import androidx.room.Database
 import androidx.room.RoomDatabase
 import androidx.room.migration.Migration
 import androidx.sqlite.db.SupportSQLiteDatabase
 import com.dino.message.chatfeature.data.db.MessageDao
 import com.dino.message.chatfeature.domain.model.MessageEntity

 @Database(entities = [MessageEntity::class], version = 5)
 abstract class MessageDatabase : RoomDatabase() {
     abstract fun messageDao(): MessageDao

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
     }
 }
