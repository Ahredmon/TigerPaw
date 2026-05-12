package com.tigerpaw.launcher.feature.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Utility for encoding a content [Uri] to a base-64 JPEG string for vision requests.
 */
object ImageEncoder {

    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 80

    /**
     * Reads the image at [uri] from the content resolver, scales it down so neither
     * dimension exceeds [MAX_DIMENSION], and returns a base-64 JPEG string.
     *
     * Returns `null` if the URI cannot be opened or decoded.
     */
    fun encodeToBase64(uri: Uri, context: Context): String? {
        return try {
            val bitmap = decodeSampled(uri, context) ?: return null
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampled(uri: Uri, context: Context): Bitmap? {
        val resolver = context.contentResolver

        // First pass: read dimensions only
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

        opts.inSampleSize = computeSampleSize(opts.outWidth, opts.outHeight)
        opts.inJustDecodeBounds = false

        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun computeSampleSize(width: Int, height: Int): Int {
        var size = 1
        while (width / (size * 2) >= MAX_DIMENSION || height / (size * 2) >= MAX_DIMENSION) {
            size *= 2
        }
        return size
    }
}
