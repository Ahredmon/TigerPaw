package com.tigerpaw.launcher.core.data.di

import android.content.Context
import androidx.room.Room
import com.tigerpaw.launcher.core.data.db.AppLaunchDao
import com.tigerpaw.launcher.core.data.db.LauncherDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LauncherDatabase =
        Room.databaseBuilder(context, LauncherDatabase::class.java, "launcher.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAppLaunchDao(db: LauncherDatabase): AppLaunchDao = db.appLaunchDao()
}
