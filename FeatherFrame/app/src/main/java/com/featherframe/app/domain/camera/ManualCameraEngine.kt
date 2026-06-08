package com.featherframe.app.domain.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.SurfaceView
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

/**
 * Low-level Camera2 engine implementing fully manual exposure controls
 * and uncompressed RAW (.dng) capture output.
 *
 * Manual overrides:
 * - ISO Sensitivity: CaptureRequest.SENSOR_SENSITIVITY (Int)
 * - Shutter Speed: CaptureRequest.SENSOR_EXPOSURE_TIME (Long nanoseconds)
 * - Focus Distance: CaptureRequest.LENS_FOCUS_DISTANCE (Float)
 * - Output Type: ImageReader with ImageFormat.RAW_SENSOR → DngCreator
 *
 * White Balance Temperature: CaptureRequest.COLOR_CORRECTION_MODE + COLOR_CORRECTION_GAINS
 * (Added for full manual control)
 */
class ManualCameraEngine(private val context: Context) {

    companion object {
        private const val TAG = "ManualCameraEngine"

        // Shutter speed ranges (in nanoseconds)
        val SHUTTER_SPEEDS = mapOf(
            "1/8000" to 125_000L,
            "1/4000" to 250_000L,
            "1/2000" to 500_000L,
            "1/1000" to 1_000_000L,
            "1/500" to 2_000_000L,
            "1/250" to 4_000_000L,
            "1/125" to 8_000_000L,
            "1/60" to 16_666_667L,
            "1/30" to 33_333_333L
        )

        // Common ISO values
        val ISO_VALUES = listOf(100, 200, 400, 800, 1600, 3200, 6400, 12800)

        // White balance temperature range (Kelvin)
        const val MIN_WB_TEMP = 2500
        const val MAX_WB_TEMP = 10000
        const val DEFAULT_WB_TEMP = 5500
    }

    // Camera2 components
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraId: String? = null

    // Capture session
    private var previewRequest: CaptureRequest? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var captureCallback: CameraCaptureSession.CaptureCallback? = null

    // Background handler thread
    private var backgroundHandlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // RAW capture
    private var rawImageReader: ImageReader? = null
    private var rawCaptureSurface: Surface? = null
    private var dngOutputDir: File? = null

    // Manual control state
    var currentIso: Int = 400
        private set
    var currentShutterSpeed: Long = 1_000_000L // 1/1000s default
        private set
    var currentFocusDistance: Float = 0.0f // auto-focus default
        private set
    var currentWhiteBalance: Int = DEFAULT_WB_TEMP
        private set
    var isManualMode: Boolean = true
        private set

    // Preview surface
    private var previewSurfaceView: SurfaceView? = null

    // Callback
    var onCaptureComplete: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onAeModeChanged: ((Boolean) -> Unit)? = null

    /**
     * Initialize the camera engine.
     */
    fun initialize(
        surfaceView: SurfaceView,
        dngOutputDir: File,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        this.previewSurfaceView = surfaceView
        this.dngOutputDir = dngOutputDir

        if (!dngOutputDir.exists()) {
            dngOutputDir.mkdirs()
        }

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        startBackgroundThread()
        openCamera(onReady, onError)
    }

    /**
     * Start the background handler thread.
     */
    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraBackground").also {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }

    /**
     * Stop the background handler thread.
     */
    private fun stopBackgroundThread() {
        backgroundHandlerThread?.quitSafely()
        try {
            backgroundHandlerThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Failed to join background thread", e)
        }
        backgroundHandlerThread = null
        backgroundHandler = null
    }

    /**
     * Open the best available camera (prefer rear RAW-capable camera).
     */
    private fun openCamera(onReady: () -> Unit, onError: (String) -> Unit) {
        try {
            cameraId = getBestCameraId()

            if (cameraId == null) {
                onError("No RAW-capable camera found")
                return
            }

            val characteristics = cameraManager?.getCameraCharacteristics(cameraId!!)
            val configs = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // Set up RAW ImageReader
            val rawSizes = configs?.getOutputSizes(ImageFormat.RAW_SENSOR)
            val rawSize = rawSizes?.firstOrNull() ?: Size(4032, 3024) // fallback size

            rawImageReader = ImageReader.newInstance(rawSize.width, rawSize.height, ImageFormat.RAW_SENSOR, 2)
            rawImageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                image?.let { saveRawImage(it) }
            }, backgroundHandler!!)
            rawCaptureSurface = rawImageReader?.surface

            // Open camera
            val callback = object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createPreviewSession(onReady, onError)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    onError("Camera error: $error")
                }
            }

            cameraManager?.openCamera(cameraId!!, callback, backgroundHandler)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera", e)
            onError("Camera access denied: ${e.message}")
        }
    }

    /**
     * Find the best camera ID supporting RAW capture.
     */
    private fun getBestCameraId(): String? {
        try {
            val cameraIds = cameraManager?.cameraIdList ?: return null

            // Prefer rear camera with RAW support
            for (id in cameraIds) {
                val characteristics = cameraManager?.getCameraCharacteristics(id) ?: continue
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

                if (facing == CameraCharacteristics.LENS_FACING_BACK &&
                    capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
                ) {
                    return id
                }
            }

            // Fallback: any RAW-capable camera
            for (id in cameraIds) {
                val characteristics = cameraManager?.getCameraCharacteristics(id) ?: continue
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

                if (capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true) {
                    return id
                }
            }

            return cameraIds.firstOrNull()
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to get camera IDs", e)
            return null
        }
    }

    /**
     * Create the preview capture session.
     */
    private fun createPreviewSession(onReady: () -> Unit, onError: (String) -> Unit) {
        try {
            val device = cameraDevice ?: return

            val surface = previewSurfaceView?.holder?.surface ?: return

            // Create preview request
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            val surfaces = mutableListOf(surface)
            rawCaptureSurface?.let { surfaces.add(it) }

            previewRequestBuilder?.addTarget(surface)

            // Set default manual controls
            applyManualSettings(previewRequestBuilder!!)

            previewRequest = previewRequestBuilder?.build()

            device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    try {
                        session.setRepeatingRequest(previewRequest!!, null, backgroundHandler)
                        onReady()
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, "Failed to start preview", e)
                        onError("Failed to start preview")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    onError("Camera configuration failed")
                }
            }, backgroundHandler)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create preview session", e)
            onError("Failed to create preview session")
        }
    }

    /**
     * Apply manual camera settings to a request builder.
     */
    private fun applyManualSettings(builder: CaptureRequest.Builder) {
        if (isManualMode) {
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF)
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentShutterSpeed)
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocusDistance)

            // Manual white balance
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
            builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
            // Apply white balance gains (simplified: use temperature to compute gain factors)
            val wbGains = computeWhiteBalanceGains(currentWhiteBalance)
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, wbGains)
        } else {
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)
        }
    }

    /**
     * Compute white balance color correction gains from temperature (Kelvin).
     * Uses a simplified approximation.
     */
    private fun computeWhiteBalanceGains(tempKelvin: Int): android.hardware.camera2.params.RggbChannelVector {
        // Normalize temperature: 2500K=reddish, 5500K=neutral, 10000K=bluish
        val normalized = (tempKelvin - MIN_WB_TEMP).toFloat() / (MAX_WB_TEMP - MIN_WB_TEMP)
        // Red gain: higher at low temps (warm), lower at high temps (cool)
        val redGain = 2.0f - normalized * 1.2f
        // Blue gain: lower at low temps, higher at high temps
        val blueGain = 1.0f + normalized * 1.2f
        val greenGain = 1.0f

        return android.hardware.camera2.params.RggbChannelVector(redGain, greenGain, greenGain, blueGain)
    }

    // ============================================================
    // Manual Control Setters
    // ============================================================

    /**
     * Set ISO sensitivity. Range typically 100–12800.
     */
    fun setIso(iso: Int) {
        if (iso < 50 || iso > 25600) return
        currentIso = iso
        isManualMode = true
        updatePreviewRequest()
    }

    /**
     * Set shutter speed in nanoseconds.
     * Use predefined values from SHUTTER_SPEEDS map or custom values.
     */
    fun setShutterSpeed(nanos: Long) {
        if (nanos < 100_000L || nanos > 60_000_000_000L) return // 1/10000s to 60s
        currentShutterSpeed = nanos
        isManualMode = true
        updatePreviewRequest()
    }

    /**
     * Set shutter speed by preset label (e.g., "1/2000").
     */
    fun setShutterSpeedByLabel(label: String): Boolean {
        val nanos = SHUTTER_SPEEDS[label] ?: return false
        setShutterSpeed(nanos)
        return true
    }

    /**
     * Set lens focus distance. 0.0f = infinity / auto, 10.0f = closest.
     */
    fun setFocusDistance(distance: Float) {
        currentFocusDistance = distance.coerceIn(0.0f, 10.0f)
        isManualMode = true
        updatePreviewRequest()
    }

    /**
     * Set manual white balance temperature in Kelvin (2500K–10000K).
     */
    fun setWhiteBalance(tempKelvin: Int) {
        currentWhiteBalance = tempKelvin.coerceIn(MIN_WB_TEMP, MAX_WB_TEMP)
        isManualMode = true
        updatePreviewRequest()
    }

    /**
     * Toggle between manual and auto exposure mode.
     */
    fun toggleAutoExposure() {
        isManualMode = !isManualMode
        if (!isManualMode) {
            // Reset manual values to auto
            currentIso = 0
            currentShutterSpeed = 0L
        }
        updatePreviewRequest()
        onAeModeChanged?.invoke(isManualMode)
    }

    /**
     * Update the preview request with current settings.
     */
    private fun updatePreviewRequest() {
        try {
            val builder = previewRequestBuilder ?: return
            applyManualSettings(builder)
            val request = builder.build()
            cameraCaptureSession?.setRepeatingRequest(request, null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to update preview request", e)
        }
    }

    // ============================================================
    // Capture
    // ============================================================

    /**
     * Capture a RAW .dng photo with current manual settings.
     */
    fun capturePhoto() {
        try {
            val device = cameraDevice ?: return

            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            applyManualSettings(captureBuilder)

            // Add RAW target
            rawCaptureSurface?.let { captureBuilder.addTarget(it) }

            // Add preview surface for JPEG preview generation
            previewSurfaceView?.holder?.surface?.let { captureBuilder.addTarget(it) }

            val captureRequest = captureBuilder.build()

            cameraCaptureSession?.capture(captureRequest, object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    Log.d(TAG, "RAW capture completed")
                }

                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                    super.onCaptureFailed(session, request, failure)
                    Log.e(TAG, "RAW capture failed: ${failure.reason}")
                    onError?.invoke("Capture failed: ${failure.reason}")
                }
            }, backgroundHandler)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to capture photo", e)
            onError?.invoke("Failed to capture photo")
        }
    }

    /**
     * Save RAW image from ImageReader as .dng file using DngCreator.
     */
    private fun saveRawImage(image: Image) {
        try {
            val dngDir = dngOutputDir ?: return
            val cameraId = cameraId ?: return
            val characteristics = cameraManager?.getCameraCharacteristics(cameraId) ?: return

            // Generate unique filename
            val fileName = "RAW_${UUID.randomUUID().toString().take(8)}.dng"
            val dngFile = File(dngDir, fileName)

            val dngCreator = DngCreator(characteristics, image)

            FileOutputStream(dngFile).use { outputStream ->
                dngCreator.writeImage(outputStream)
                outputStream.flush()
            }

            dngCreator.close()
            image.close()

            Log.i(TAG, "DNG saved: ${dngFile.absolutePath}")
            onCaptureComplete?.invoke(dngFile)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save RAW image", e)
            onError?.invoke("Failed to save RAW file")
        } finally {
            image.close()
        }
    }

    // ============================================================
    // Lifecycle
    // ============================================================

    /**
     * Release camera resources.
     */
    fun release() {
        try {
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
            rawImageReader?.close()
            rawImageReader = null
            stopBackgroundThread()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera", e)
        }
    }
}
