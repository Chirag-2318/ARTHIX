package com.chirag.arthix.di

import com.chirag.arthix.data.repository.PendingQueueRepository
import com.chirag.arthix.data.repository.ReportRepository
import com.chirag.arthix.data.repository.SplitRepository
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.data.repository.impl.PendingQueueRepositoryImpl
import com.chirag.arthix.data.repository.impl.ReportRepositoryImpl
import com.chirag.arthix.data.repository.impl.SplitRepositoryImpl
import com.chirag.arthix.data.repository.impl.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to their concrete implementations.
 *
 * All repositories are @Singleton — they hold no mutable state themselves
 * (state lives in Room), but creating multiple instances would waste memory
 * and defeat Flow sharing.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindSplitRepository(
        impl: SplitRepositoryImpl
    ): SplitRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(
        impl: ReportRepositoryImpl
    ): ReportRepository

    @Binds
    @Singleton
    abstract fun bindPendingQueueRepository(
        impl: PendingQueueRepositoryImpl
    ): PendingQueueRepository
}
