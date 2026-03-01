package com.anomalyzed.docscanner.core.di

import com.anomalyzed.docscanner.data.repository.DocumentRepositoryImpl
import com.anomalyzed.docscanner.domain.repository.IDocumentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        documentRepositoryImpl: DocumentRepositoryImpl
    ): IDocumentRepository
}
