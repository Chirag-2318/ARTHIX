package com.chirag.arthix.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chirag.arthix.data.dao.PendingQueueDao
import com.chirag.arthix.data.dao.ReportDao
import com.chirag.arthix.data.dao.SplitDao
import com.chirag.arthix.data.dao.TransactionDao
import com.chirag.arthix.data.entity.PendingCaptureEntity
import com.chirag.arthix.data.entity.PendingNotificationEntity
import com.chirag.arthix.data.entity.ReportEntity
import com.chirag.arthix.data.entity.SplitParticipantEntity
import com.chirag.arthix.data.entity.SplitRecordEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.entity.BudgetStreakEntity
import com.chirag.arthix.data.entity.StreakDailyCapEntity
import com.chirag.arthix.data.dao.BudgetStreakDao
import com.chirag.arthix.data.model.EnumConverters
import com.chirag.arthix.data.model.JsonConverters

/**
 * Arthix Room database — the single source of truth for all persisted data.
 *
 * Configuration notes (PRD §5, §8):
 * - WAL mode is explicitly enabled (not relied on as an implicit default).
 * - [fallbackToDestructiveMigration] is a conscious, stated choice for the
 *   hackathon build cycle — there is no production install base to preserve.
 *   This is documented so it reads as a decision, not an oversight.
 *
 * Builder usage:
 * ```kotlin
 * val db = Room.databaseBuilder(context, ArthixDatabase::class.java, "arthix.db")
 *     .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
 *     .fallbackToDestructiveMigration()
 *     .build()
 * ```
 */
@Database(
    entities = [
        TransactionEntity::class,
        PendingCaptureEntity::class,
        PendingNotificationEntity::class,
        SplitRecordEntity::class,
        SplitParticipantEntity::class,
        ReportEntity::class,
        BudgetStreakEntity::class,
        StreakDailyCapEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(EnumConverters::class, JsonConverters::class)
abstract class ArthixDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun pendingQueueDao(): PendingQueueDao
    abstract fun splitDao(): SplitDao
    abstract fun reportDao(): ReportDao
    abstract fun budgetStreakDao(): BudgetStreakDao

    companion object {
        const val DATABASE_NAME = "arthix.db"
        
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add columns to pending_notifications
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'UPI_APP_NOTIFICATION'")
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN senderAddress TEXT")
                
                // Add columns to transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceType TEXT")
            }
        }
    }
}
