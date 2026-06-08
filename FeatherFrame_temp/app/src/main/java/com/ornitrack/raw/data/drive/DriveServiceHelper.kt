package com.ornitrack.raw.data.drive

import android.content.Context
import android.os.Environment
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as JavaFile
import java.util.Collections

/**
 * Google Drive REST API multi-part upload engine for RAW (.dng) files.
 *
 * File naming rule (as per spec):
 *   val finalDestinationFileName: String = "${activeBirdId}_${activePhotographerId}.dng"
 *   // Output Example: BRD_042_PHOTO_7781.dng
 *
 * Uses GoogleAuthHelper for OAuth2 authentication.
 */
class DriveServiceHelper(private val context: Context) {

    companion object {
        private const val APPLICATION_NAME = "OrniTrackRAW"
        private const val RAW_MIME_TYPE = "image/x-adobe-dng"
        private const val JPEG_MIME_TYPE = "image/jpeg"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val ORNITRACK_FOLDER_NAME = "OrniTrackRAW_Captures"
    }

    private var driveService: Drive? = null

    /**
     * Initialize the Drive service using GoogleAuthHelper's authenticated service.
     */
    suspend fun initialize(googleAuthHelper: com.ornitrack.raw.domain.auth.GoogleAuthHelper): Boolean = withContext(Dispatchers.IO) {
        try {
            driveService = googleAuthHelper.getDriveService()
            if (driveService != null) {
                android.util.Log.i("DriveServiceHelper", "Drive service initialized from GoogleAuthHelper")
                true
            } else {
                android.util.Log.w("DriveServiceHelper", "No authenticated Drive service available")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DriveServiceHelper", "Failed to initialize Drive service", e)
            false
        }
    }

    /**
     * Dynamic file naming rule:
     *   finalDestinationFileName = "${activeBirdId}_${activePhotographerId}.dng"
     */
    fun generateFileName(birdId: String, photographerId: String): String {
        return "${birdId}_${photographerId}.dng"
    }

    /**
     * Find or create the app's dedicated folder on Google Drive.
     */
    private suspend fun getOrCreateAppFolder(): String? = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext null

        try {
            // Search for existing folder
            val query = "mimeType='$FOLDER_MIME_TYPE' and name='$ORNITRACK_FOLDER_NAME' and trashed=false"
            val result = service.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }

            // Create folder
            val folderMetadata = File().apply {
                name = ORNITRACK_FOLDER_NAME
                mimeType = FOLDER_MIME_TYPE
            }
            val folder = service.files().create(folderMetadata)
                .setFields("id")
                .execute()
            folder.id
        } catch (e: Exception) {
            android.util.Log.e("DriveServiceHelper", "Failed to get/create folder", e)
            null
        }
    }

    /**
     * Upload a RAW .dng file to Google Drive with the dynamic naming convention.
     *
     * @param birdId The active bird species ID (e.g., "BRD_042")
     * @param photographerId The photographer's ID
     * @param localDngFile The local .dng file to upload
     * @return The Google Drive file ID or null on failure
     */
    suspend fun uploadRawFile(
        birdId: String,
        photographerId: String,
        localDngFile: JavaFile
    ): String? = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext null

        try {
            val folderId = getOrCreateAppFolder() ?: return@withContext null

            // Generate dynamic file name per spec: "${activeBirdId}_${activePhotographerId}.dng"
            val fileName = generateFileName(birdId, photographerId)

            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }

            val mediaContent = FileContent(RAW_MIME_TYPE, localDngFile)

            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name")
                .execute()

            android.util.Log.i(
                "DriveServiceHelper",
                "Uploaded RAW file: ${uploadedFile.name} (ID: ${uploadedFile.id})"
            )

            uploadedFile.id
        } catch (e: Exception) {
            android.util.Log.e("DriveServiceHelper", "Failed to upload RAW file", e)
            null
        }
    }

    /**
     * Upload a JPEG preview thumbnail to accompany the RAW capture.
     */
    suspend fun uploadPreviewJpeg(
        birdId: String,
        photographerId: String,
        localJpegFile: JavaFile
    ): String? = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext null

        try {
            val folderId = getOrCreateAppFolder() ?: return@withContext null

            val fileName = "${birdId}_${photographerId}_preview.jpg"

            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
                description = "JPEG preview for $birdId capture by $photographerId"
            }

            val mediaContent = FileContent(JPEG_MIME_TYPE, localJpegFile)

            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, webContentLink")
                .execute()

            uploadedFile.id
        } catch (e: Exception) {
            android.util.Log.e("DriveServiceHelper", "Failed to upload JPEG preview", e)
            null
        }
    }

    /**
     * List all uploaded RAW files in the app folder.
     */
    suspend fun listUploadedRaws(): List<File> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext emptyList()

        try {
            val folderId = getOrCreateAppFolder() ?: return@withContext emptyList()
            val query = "'$folderId' in parents and mimeType='$RAW_MIME_TYPE' and trashed=false"

            val result = service.files().list()
                .setQ(query)
                .setFields("files(id, name, size, createdTime)")
                .execute()

            result.files ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("DriveServiceHelper", "Failed to list RAW files", e)
            emptyList()
        }
    }
}