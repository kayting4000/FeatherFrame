package com.ornitrack.raw.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * On-device ML Kit / TFLite bird classification engine.
 *
 * Loads a localized machine learning model to parse camera stream frames
 * and provide real-time species recommendations onto the camera viewfinder
 * screen — completely offline.
 */
class BirdClassifier(private val context: Context) {

    companion object {
        private const val TAG = "BirdClassifier"
        private const val CONFIDENCE_THRESHOLD = 0.6f
    }

    // Use ML Kit's built-in image labeling (can be replaced with custom TFLite model)
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build()
    )

    /**
     * Represents a bird species classification result.
     */
    data class ClassificationResult(
        val speciesName: String,
        val scientificName: String? = null,
        val confidence: Float,
        val birdId: String? = null
    )

    /**
     * Classify a bitmap frame from the camera preview.
     * Runs ML inference on the UI thread safely via coroutines.
     *
     * @param bitmap The camera preview frame bitmap
     * @return List of classification results sorted by confidence (descending)
     */
    suspend fun classifyFrame(bitmap: Bitmap): List<ClassificationResult> =
        withContext(Dispatchers.Default) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val labels: List<ImageLabel> = labeler.process(inputImage).await()

                labels.mapNotNull { label ->
                    // Map ML Kit labels to bird species format
                    val birdId = mapLabelToBirdId(label.text)
                    if (birdId != null || label.confidence >= CONFIDENCE_THRESHOLD) {
                        ClassificationResult(
                            speciesName = label.text,
                            confidence = label.confidence,
                            birdId = birdId
                        )
                    } else null
                }.sortedByDescending { it.confidence }
            } catch (e: Exception) {
                Log.e(TAG, "Classification failed", e)
                emptyList()
            }
        }

    /**
     * Classify from a raw byte array (NV21 or YUV_420_888 format from camera preview).
     *
     * @param data Camera preview data bytes
     * @param width Preview width
     * @param height Preview height
     * @param rotation Degrees rotation
     * @return List of classification results
     */
    suspend fun classifyPreviewFrame(
        data: ByteArray,
        width: Int,
        height: Int,
        rotation: Int = 0
    ): List<ClassificationResult> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromByteArray(data, width, height, rotation, InputImage.IMAGE_FORMAT_NV21)
            val labels: List<ImageLabel> = labeler.process(inputImage).await()

            labels.mapNotNull { label ->
                val birdId = mapLabelToBirdId(label.text)
                if (birdId != null || label.confidence >= CONFIDENCE_THRESHOLD) {
                    ClassificationResult(
                        speciesName = label.text,
                        confidence = label.confidence,
                        birdId = birdId
                    )
                } else null
            }.sortedByDescending { it.confidence }
        } catch (e: Exception) {
            Log.e(TAG, "Preview classification failed", e)
            emptyList()
        }
    }

    /**
     * Map ML Kit label text to standardized bird ID format.
     * In production, this would use a loaded TFLite model's label map.
     * For now, we generate IDs from the common name.
     */
    private fun mapLabelToBirdId(labelText: String): String? {
        if (labelText.isBlank()) return null
        // Generate a consistent ID from the label text
        val shortCode = labelText
            .take(3)
            .uppercase()
            .replace(Regex("[^A-Z]"), "")
        return if (shortCode.isNotBlank()) "BRD_$shortCode" else null
    }

    /**
     * Get top-3 suggestions for the viewfinder overlay when confidence is low.
     */
    suspend fun getTopSuggestions(bitmap: Bitmap): List<ClassificationResult> {
        val results = classifyFrame(bitmap)
        return if (results.isNotEmpty() && results.first().confidence < 0.8f) {
            results.take(3)
        } else {
            results.take(1)
        }
    }

    /**
     * Release the ML Kit resources.
     */
    fun close() {
        labeler.close()
    }
}