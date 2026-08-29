package com.chirag.arthix.di

import android.content.Context
import androidx.room.Room
import com.chirag.arthix.data.ArthixDatabase
import com.chirag.arthix.data.dao.PendingQueueDao
import com.chirag.arthix.data.dao.ReportDao
import com.chirag.arthix.data.dao.SplitDao
import com.chirag.arthix.data.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAO instances.
 *
 * Scoped @Singleton — one database instance for the entire app lifetime.
 * WAL mode is explicitly enabled (not relied on as default).
 * fallbackToDestructiveMigration is a conscious hackathon-timeline choice.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArthixDatabase {
        return Room.databaseBuilder(
            context,
            ArthixDatabase::class.java,
            ArthixDatabase.DATABASE_NAME
        )
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(ArthixDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(db: ArthixDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun providePendingQueueDao(db: ArthixDatabase): PendingQueueDao = db.pendingQueueDao()

    @Provides
    fun provideSplitDao(db: ArthixDatabase): SplitDao = db.splitDao()

    @Provides
    fun provideReportDao(db: ArthixDatabase): ReportDao = db.reportDao()

    @Provides
    @Singleton
    fun provideReconciliationEngine(
        db: ArthixDatabase,
        @ApplicationContext context: Context
    ): com.chirag.arthix.notification.ReconciliationEngine {
        val chipTrigger = com.chirag.arthix.sensor.HeadsUpChipTrigger(context)
        return com.chirag.arthix.notification.ReconciliationEngine(
            database = db,
            chipTrigger = chipTrigger
        )
    }
}
