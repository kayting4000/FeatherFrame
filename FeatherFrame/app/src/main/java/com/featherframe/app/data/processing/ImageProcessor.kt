package com.featherframe.app.data.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Local pipeline for extracting high-quality JPEG previews from RAW sensor buffers.
 *
 * Because .dng files cannot be displayed directly on the newsfeed, this processor
 * reads the uncompressed sensor buffer, downsamples it, applies orientation,
 * and outputs a compressed .jpg image asset to cache.
 */
class ImageProcessor {

    companion object {
        private const val JPEG_QUALITY = 92
        private const val PREVIEW_MAX_DIMENSION = 2048
        private const val TAG = "ImageProcessor"
    }

    /**
     * Convert a RAW .dng file to a compressed JPEG preview.
     *
     * @param dngFile The source DNG file
     * @param outputDir Directory to write the JPEG preview
     * @return The output JPEG file, or null on failure
     */
    suspend fun convertDngToJpegPreview(
        dngFile: File,
        outputDir: File
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Read the DNG as a Bitmap — DNG is a standard format Android can decode
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(dngFile.absolutePath, options)

            // Calculate downscale factor to keep within PREVIEW_MAX_DIMENSION
            val scaleFactor = calculateInSampleSize(
                options.outWidth, options.outHeight, PREVIEW_MAX_DIMENSION
            )

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap = BitmapFactory.decodeFile(dngFile.absolutePath, decodeOptions)
                ?: return@withContext null

            // Apply orientation from EXIF
            val rotatedBitmap = applyOrientation(rawBitmap, dngFile)
            if (rotatedBitmap != rawBitmap) {
                rawBitmap.recycle()
            }

            // Generate output filename
            val jpegFile = File(outputDir, "${dngFile.nameWithoutExtension}_preview.jpg")

            FileOutputStream(jpegFile).use { outputStream ->
                val bitmap = rotatedBitmap ?: rawBitmap
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                outputStream.flush()

                if (rotatedBitmap != null && rotatedBitmap != rawBitmap) {
                    rotatedBitmap.recycle()
                }
            }

            android.util.Log.i(TAG, "JPEG preview created: ${jpegFile.absolutePath}")
            jpegFile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to convert DNG to JPEG", e)
            null
        }
    }

    /**
     * Process a raw sensor byte array directly from the camera (e.g., from ImageReader).
     * Saves a JPEG to the given output path.
     *
     * @param rawBytes Raw sensor data bytes
     * @param width Sensor image width
     * @param height Sensor image height
     * @param outputFile Destination JPEG file
     * @return true if successful
     */
    suspend fun processRawBufferToJpeg(
        rawBytes: ByteArray,
        width: Int,
        height: Int,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(width, height, PREVIEW_MAX_DIMENSION)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                ?: return@withContext false

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
                bitmap.recycle()
            }

            android.util.Log.i(TAG, "Raw buffer processed to JPEG: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to process raw buffer", e)
            false
        }
    }

    /**
     * Calculate a sample size that keeps the image within maxDimension on the longest side.
     */
    private fun calculateInSampleSize(
        width: Int, height: Int, maxDimension: Int
    ): Int {
        var inSampleSize = 1
        if (height > maxDimension || width > maxDimension) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= maxDimension &&
                halfWidth / inSampleSize >= maxDimension
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Apply EXIF rotation to ensure the preview has correct orientation.
     */
    private fun applyOrientation(bitmap: Bitmap, file: File): Bitmap? {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }

            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            rotated
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to apply orientation", e)
            bitmap
        }
    }
}
