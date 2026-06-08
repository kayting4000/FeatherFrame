package com.ornitrack.raw.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) for Supabase/PostgreSQL remote sync
 * and Room Entity mappings for local offline-first storage.
 */

// ============================================================
// Remote DTOs (for Retrofit/Supabase serialization)
// ============================================================

data class PhotographerDTO(
    @SerializedName("photographer_id")
    val photographerId: String = "",

    @SerializedName("full_name")
    val fullName: String = "",

    @SerializedName("email")
    val email: String = "",

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("bio")
    val bio: String? = null,

    @SerializedName("favorite_gear")
    val favoriteGear: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

data class BirdDTO(
    @SerializedName("bird_id")
    val birdId: String = "",

    @SerializedName("species_name")
    val speciesName: String = "",

    @SerializedName("scientific_name")
    val scientificName: String? = null
)

data class BirdCaptureDTO(
    @SerializedName("capture_id")
    val captureId: String = "",

    @SerializedName("photographer_id")
    val photographerId: String = "",

    @SerializedName("bird_id")
    val birdId: String = "",

    @SerializedName("gdrive_file_id")
    val gdriveFileId: String? = null,

    @SerializedName("preview_jpeg_url")
    val previewJpegUrl: String = "",

    @SerializedName("latitude")
    val latitude: Double = 0.0,

    @SerializedName("longitude")
    val longitude: Double = 0.0,

    @SerializedName("captured_at")
    val capturedAt: String? = null
)

data class CaptureLikeDTO(
    @SerializedName("like_id")
    val likeId: String = "",

    @SerializedName("capture_id")
    val captureId: String = "",

    @SerializedName("photographer_id")
    val photographerId: String = ""
)

data class CaptureCommentDTO(
    @SerializedName("comment_id")
    val commentId: String = "",

    @SerializedName("capture_id")
    val captureId: String = "",

    @SerializedName("photographer_id")
    val photographerId: String = "",

    @SerializedName("comment_text")
    val commentText: String = "",

    @SerializedName("created_at")
    val createdAt: String? = null
)

// ============================================================
// Local Room Entities (with is_synced flag for offline management)
// ============================================================

@Entity(tableName = "photographers")
data class PhotographerEntity(
    @PrimaryKey
    @SerializedName("photographer_id")
    val photographerId: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("bio")
    val bio: String? = null,

    @SerializedName("favorite_gear")
    val favoriteGear: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    val isSynced: Boolean = true
)

@Entity(tableName = "birds")
data class BirdEntity(
    @PrimaryKey
    @SerializedName("bird_id")
    val birdId: String,

    @SerializedName("species_name")
    val speciesName: String,

    @SerializedName("scientific_name")
    val scientificName: String? = null,

    val isSynced: Boolean = true
)

@Entity(
    tableName = "bird_captures",
    foreignKeys = [
        ForeignKey(
            entity = PhotographerEntity::class,
            parentColumns = ["photographerId"],
            childColumns = ["photographerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BirdEntity::class,
            parentColumns = ["birdId"],
            childColumns = ["birdId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("photographerId"),
        Index("birdId")
    ]
)
data class BirdCaptureEntity(
    @PrimaryKey
    @SerializedName("capture_id")
    val captureId: String,

    @SerializedName("photographer_id")
    val photographerId: String,

    @SerializedName("bird_id")
    val birdId: String,

    @SerializedName("gdrive_file_id")
    val gdriveFileId: String? = null,

    @SerializedName("preview_jpeg_url")
    val previewJpegUrl: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("captured_at")
    val capturedAt: String? = null,

    val isSynced: Boolean = false
)

@Entity(
    tableName = "capture_likes",
    foreignKeys = [
        ForeignKey(
            entity = BirdCaptureEntity::class,
            parentColumns = ["captureId"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PhotographerEntity::class,
            parentColumns = ["photographerId"],
            childColumns = ["photographerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index("photographerId")
    ]
)
data class CaptureLikeEntity(
    @PrimaryKey
    @SerializedName("like_id")
    val likeId: String,

    @SerializedName("capture_id")
    val captureId: String,

    @SerializedName("photographer_id")
    val photographerId: String,

    val isSynced: Boolean = false
)

@Entity(
    tableName = "capture_comments",
    foreignKeys = [
        ForeignKey(
            entity = BirdCaptureEntity::class,
            parentColumns = ["captureId"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PhotographerEntity::class,
            parentColumns = ["photographerId"],
            childColumns = ["photographerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("captureId"),
        Index("photographerId")
    ]
)
data class CaptureCommentEntity(
    @PrimaryKey
    @SerializedName("comment_id")
    val commentId: String,

    @SerializedName("capture_id")
    val captureId: String,

    @SerializedName("photographer_id")
    val photographerId: String,

    @SerializedName("comment_text")
    val commentText: String,

    @SerializedName("created_at")
    val createdAt: String? = null,

    val isSynced: Boolean = false
)

