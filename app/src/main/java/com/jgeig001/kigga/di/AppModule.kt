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
        Log.d("APP_MODULE", "provideModelWrapper")
        return ModelWrapper(user, liga, history)
    }

    @Singleton
    @Provides
    fun provideUser(): User {
        Log.d("APP_MODULE", "provideUser")
        return User("Mr.Dummy", Club("1. FSV Mainz 05", "FSV Mainz"))
    }

    @Singleton
    @Provides
    fun provideLiga(): Liga {
        Log.d("APP_MODULE", "provideLiga")
        return Liga()
    }

    @Singleton
    @Provides
    fun provideHistory(persistenceManager: PersistenceManager): History {
        Log.d("APP_MODULE", "provideHistory")
        return persistenceManager.getLoadedModel().getHistory()
    }

    @Singleton
    @Provides
    fun providePersistanceManager(@ApplicationContext context: Context): PersistenceManager {
        Log.d("APP_MODULE", "providePersistanceManager")
        return PersistenceManager(context)
    }

    @Singleton
    @Provides
    fun provideSelectedSeasonIndex(@ApplicationContext context: Context): Int {
        Log.d("APP_MODULE", "provideSelectedSeasonIndex")
        return SharedPreferencesManager.getInt(context, History.SELECTED_SEASON_SP_KEY)
    }


}