package com.ornitrack.raw.ui.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ornitrack.raw.OrniTrackApp
import com.ornitrack.raw.data.database.BirdCaptureEntity
import com.ornitrack.raw.data.database.DatabaseClient
import com.ornitrack.raw.data.database.BirdCaptureDTO
import com.ornitrack.raw.data.drive.DriveServiceHelper
import com.ornitrack.raw.data.processing.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Jetpack WorkManager background queue engine for offline-first sync.
 *
 * Handles:
 * 1. Uploading unsynced RAW (.dng) files to Google Drive
 * 2. Generating JPEG previews from RAW buffers
 * 3. Syncing capture metadata to Supabase
 * 4. Marking captures as synced locally
 */
class SyncUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncUploadWorker"
        private const val WORK_NAME = "ornitrack_sync_upload"

        /**
         * Schedule periodic sync work (every 15 minutes when constraints are met).
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncUploadWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )

            Log.i(TAG, "Sync upload worker scheduled")
        }

        /**
         * Enqueue a one-time immediate sync.
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncUploadWorker>()
                .setConstraints(constraints)
                .addTag("${WORK_NAME}_immediate")
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.i(TAG, "Immediate sync requested")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Sync upload worker starting...")

        try {
            val app = applicationContext as OrniTrackApp
            val db = app.database
            val driveService = DriveServiceHelper(applicationContext)
            val imageProcessor = ImageProcessor()

            // Initialize Drive service
            val driveInitialized = driveService.initialize()
            if (!driveInitialized) {
                Log.w(TAG, "Drive service not initialized, will retry")
                return@withContext Result.retry()
            }

            // Get all unsynced captures
            val unsyncedCaptures = db.birdCaptureDao().getUnsyncedCaptures()

            if (unsyncedCaptures.isEmpty()) {
                Log.i(TAG, "No unsynced captures found")
                return@withContext Result.success()
            }

            Log.i(TAG, "Found ${unsyncedCaptures.size} unsynced captures")

            var syncedCount = 0

            for (capture in unsyncedCaptures) {
                try {
                    val syncResult = syncSingleCapture(
                        capture = capture,
                        driveService = driveService,
                        imageProcessor = imageProcessor,
                        db = db
                    )

                    if (syncResult) {
                        syncedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync capture ${capture.captureId}", e)
                }
            }

            Log.i(TAG, "Sync complete: $syncedCount/${unsyncedCaptures.size} captures synced")

            if (syncedCount < unsyncedCaptures.size) {
                // Some failed — retry
                Result.retry()
            } else {
                Result.success()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed", e)
            Result.failure()
        }
    }

    /**
     * Sync a single capture: generate JPEG preview, upload to Drive, sync to Supabase.
     */
    private suspend fun syncSingleCapture(
        capture: BirdCaptureEntity,
        driveService: DriveServiceHelper,
        imageProcessor: ImageProcessor,
        db: com.ornitrack.raw.data.database.AppDatabase
    ): Boolean {
        return try {
            // Step 1: Generate JPEG preview from local DNG if available
            val dngDir = File(applicationContext.filesDir, "dng_captures")
            val dngFile = File(dngDir, "${capture.captureId}.dng")

            var previewUrl = capture.previewJpegUrl

            if (dngFile.exists()) {
                val jpegOutputDir = File(applicationContext.cacheDir, "previews")
                if (!jpegOutputDir.exists()) jpegOutputDir.mkdirs()

                val jpegFile = imageProcessor.convertDngToJpegPreview(dngFile, jpegOutputDir)
                if (jpegFile != null) {
                    // Upload JPEG to Drive
                    val jpegDriveId = driveService.uploadPreviewJpeg(
                        birdId = capture.birdId,
                        photographerId = capture.photographerId,
                        localJpegFile = jpegFile
                    )

                    if (jpegDriveId != null) {
                        previewUrl = jpegFile.absolutePath
                    }
                }
            }

            // Step 2: Upload RAW DNG to Google Drive
            val driveFileId = if (dngFile.exists()) {
                driveService.uploadRawFile(
                    birdId = capture.birdId,
                    photographerId = capture.photographerId,
                    localDngFile = dngFile
                )
            } else null

            // Step 3: Sync capture metadata to Supabase
            try {
                val dto = BirdCaptureDTO(
                    captureId = capture.captureId,
                    photographerId = capture.photographerId,
                    birdId = capture.birdId,
                    gdriveFileId = driveFileId,
                    previewJpegUrl = previewUrl,
                    latitude = capture.latitude,
                    longitude = capture.longitude,
                    capturedAt = capture.capturedAt
                )
                DatabaseClient.supabaseApi.createBirdCapture(dto)
            } catch (e: Exception) {
                Log.w(TAG, "Supabase sync failed (will retry): ${e.message}")
            }

            // Step 4: Mark as synced locally
            db.birdCaptureDao().markAsSynced(
                captureId = capture.captureId,
                gdriveFileId = driveFileId ?: ""
            )

            Log.i(TAG, "Capture ${capture.captureId} synced successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync capture ${capture.captureId}", e)
            false
        }
    }
}