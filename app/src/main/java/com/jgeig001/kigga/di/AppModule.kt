package com.jgeig001.kigga.di

import android.content.Context
import android.util.Log
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.model.persitence.PersistenceManager
import com.jgeig001.kigga.utils.SharedPreferencesManager
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
    fun provideModelWrapper(user: User, liga: Liga, history: History): ModelWrapper {
        return ModelWrapper(user, liga, history)
    }

    @Singleton
    @Provides
    fun provideUser(): User {
        return User("Mr.Dummy", Club("1. FSV Mainz 05", "FSV Mainz"))
    }

    @Singleton
    @Provides
    fun provideLiga(): Liga {
        return Liga()
    }

    @Singleton
    @Provides
    fun provideHistory(persistenceManager: PersistenceManager): History {
        return persistenceManager.getLoadedModel().getHistory()
    }

    @Singleton
    @Provides
    fun providePersistanceManager(@ApplicationContext context: Context): PersistenceManager {
        return PersistenceManager(context)
    }

    @Singleton
    @Provides
    fun provideSelectedSeasonIndex(@ApplicationContext context: Context): Int {
        return SharedPreferencesManager.getInt(context, History.SELECTED_SEASON_SP_KEY)
    }


}