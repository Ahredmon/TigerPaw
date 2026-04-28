package com.tigerpaw.launcher.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppLaunchEntity::class], version = 3, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appLaunchDao(): AppLaunchDao
}
