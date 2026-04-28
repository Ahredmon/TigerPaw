package com.tigerpaw.launcher.core.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = WallpaperManager.getInstance(context)

    /** Returns the current home-screen wallpaper bitmap, or null if unavailable/permission denied. */
    fun getCurrentWallpaper(): Flow<Bitmap?> = flow {
        val bitmap = try {
            val drawable = manager.drawable
            if (drawable is BitmapDrawable) drawable.bitmap else null
        } catch (e: SecurityException) {
            null
        }
        emit(bitmap)
    }.flowOn(Dispatchers.IO)

    /**
     * Sets the wallpaper from a content URI (e.g. picked via photo picker).
     * Requires SET_WALLPAPER permission.
     */
    suspend fun setWallpaperFromUri(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            manager.setStream(stream)
        }
    }
}
