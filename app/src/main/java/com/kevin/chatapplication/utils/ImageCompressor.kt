package com.kevin.chatapplication.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Shrinks a picked or captured photo to at most [maxSize] px on its longest side,
 * fixes camera rotation and returns JPEG bytes ready to upload.
 * Call off the main thread.
 */
object ImageCompressor {

    fun compress(
        resolver: ContentResolver,
        uri: Uri,
        maxSize: Int = 1600,
        quality: Int = 80
    ): ByteArray? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSize ||
                bounds.outHeight / (sample * 2) >= maxSize
            ) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            decoded?.let { bitmap ->
                val upright = rotateIfNeeded(resolver, uri, bitmap)
                ByteArrayOutputStream().use { out ->
                    upright.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    out.toByteArray()
                }
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun rotateIfNeeded(resolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
