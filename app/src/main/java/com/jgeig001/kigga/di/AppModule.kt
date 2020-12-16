package com.jgeig001.kigga.di

import android.content.Context
import androidx.room.Room
import com.jgeig001.kigga.database.LocalDatabase
import com.jgeig001.kigga.model.domain.History
import com.jgeig001.kigga.model.domain.LigaClass
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.persitence.PersistenceManager
import com.jgeig001.kigga.utils.SeasonSelect
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class) // live as long as application
object AppModule {

    @Singleton
    @Provides
    fun provideModelWrapper(liga: LigaClass, history: History): ModelWrapper {
        return ModelWrapper(liga, history)
    }

    @Singleton
    @Provides
    fun provideLigaClass(persistenceManager: PersistenceManager): LigaClass {
        return persistenceManager.getLoadedModel().getLiga()
    }

    @Singleton
    @Provides
    fun provideHistory(persistenceManager: PersistenceManager): History {
        return persistenceManager.getLoadedModel().getHistory()
    }

    @Singleton
    @Provides
    fun providePersistanceManager(
        @ApplicationContext context: Context,
        db: LocalDatabase
    ): PersistenceManager {
        return PersistenceManager(context, db)
    }

    @Singleton
    @Provides
    fun provideSelectedSeasonIndex(@ApplicationContext context: Context): Int {
        return SeasonSelect.getSelectedSeasonIndex(context)
    }

    @Singleton
    @Provides
    fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            LocalDatabase::class.java,
            "local_database"
        ).build()
    }

}