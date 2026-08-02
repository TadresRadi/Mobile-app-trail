package com.fintech.vfcgateway.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PendingWebhookDao {
    @Query("SELECT * FROM pending_webhooks WHERE isProcessed = 0 ORDER BY createdAt ASC")
    fun getUnprocessed(): List<PendingWebhook>

    @Query("SELECT * FROM pending_webhooks WHERE transactionId = :txId LIMIT 1")
    fun getByTxId(txId: String): PendingWebhook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(webhook: PendingWebhook)

    @Update
    fun update(webhook: PendingWebhook)

    @Query("DELETE FROM pending_webhooks WHERE transactionId = :txId")
    fun deleteByTxId(txId: String)
}

@Database(entities = [PendingWebhook::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingWebhookDao(): PendingWebhookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vfc_gateway_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}