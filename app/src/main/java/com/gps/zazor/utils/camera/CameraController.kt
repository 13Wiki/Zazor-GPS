package com.gps.zazor.utils.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import android.hardware.camera2.CameraCharacteristics

/**
 * Thin wrapper around CameraX that gives the screens the small surface they actually use:
 * start/stop a preview, toggle the torch, flip the lens and grab a correctly rotated [Bitmap].
 *
 * Every callback is delivered on the main thread.
 */
class CameraController(
    private val context: Context,
    private val previewView: PreviewView
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: androidx.camera.core.Camera? = null

    private var lens: Camera = Camera.BACK
    private var torchEnabled: Boolean = false

    /** True once the preview is bound and a picture can be taken. */
    val isRunning get() = camera != null

    /** True when this device actually has a wider-than-normal back lens. */
    var hasUltraWide: Boolean = false
        private set

    /** Whether the wide lens is the one currently bound. */
    var isUltraWide: Boolean = false
        private set

    fun start(owner: LifecycleOwner, lens: Camera = this.lens, onError: (Throwable) -> Unit = {}) {
        this.lens = lens
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                bind(owner, future.get())
            } catch (e: Exception) {
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
    }

    /** Rebinds to the opposite lens. Returns the lens now in use. */
    fun flip(owner: LifecycleOwner, onError: (Throwable) -> Unit = {}): Camera {
        lens = lens.flipped()
        cameraProvider?.let { bindSafely(owner, it, onError) } ?: start(owner, lens, onError)
        return lens
    }

    /**
     * Switches between the normal and the widest back lens.
     *
     * Replaces the stitched panorama for the common case: on a modern phone the ultra-wide covers
     * roughly 120 degrees in a single frame, which is instant, needs no stitching library, and
     * carries exactly the same stamp as any other photo. Returns the lens now in use.
     */
    fun setUltraWide(owner: LifecycleOwner, wide: Boolean, onError: (Throwable) -> Unit = {}): Boolean {
        if (wide == isUltraWide) return isUltraWide
        if (wide && !hasUltraWide) return false
        isUltraWide = wide
        cameraProvider?.let { bindSafely(owner, it, onError) } ?: start(owner, lens, onError)
        return isUltraWide
    }

    fun setTorch(enabled: Boolean) {
        torchEnabled = enabled
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(enabled)
    }

    fun takePicture(onResult: (Bitmap) -> Unit, onError: (Throwable) -> Unit = {}) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera is not started"))
            return
        }
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        onResult(image.toUprightBitmap(mirrored = lens == Camera.FRONT))
                    } catch (e: Exception) {
                        onError(e)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError.invoke(exception)
                }
            }
        )
    }

    private fun bind(owner: LifecycleOwner, provider: ProcessCameraProvider) {
        cameraProvider = provider
        hasUltraWide = provider.findWidestBackLens() != null
        bindSafely(owner, provider) { throw it }
    }

    /**
     * The widest back lens, by shortest focal length.
     *
     * CameraX has no "ultra-wide" concept, so the physical cameras are inspected through the
     * Camera2 interop and the shortest focal length wins. A device with a single back camera
     * returns null and the wide mode is simply not offered.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    private fun ProcessCameraProvider.findWidestBackLens(): CameraSelector? {
        val backCameras = try {
            availableCameraInfos.filter {
                Camera2CameraInfo.from(it)
                    .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            return null
        }
        if (backCameras.size < 2) return null

        val widest = backCameras.minByOrNull { it.shortestFocalLength() ?: Float.MAX_VALUE }
            ?: return null
        val default = backCameras.maxByOrNull { it.shortestFocalLength() ?: 0F }
        // Two cameras with the same focal length are not a wide/normal pair.
        if (widest.shortestFocalLength() == default?.shortestFocalLength()) return null

        val widestId = Camera2CameraInfo.from(widest).cameraId
        return CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .addCameraFilter(CameraFilter { infos ->
                infos.filter { Camera2CameraInfo.from(it).cameraId == widestId }
            })
            .build()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    private fun CameraInfo.shortestFocalLength(): Float? = try {
        Camera2CameraInfo.from(this)
            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.minOrNull()
    } catch (e: Exception) {
        null
    }

    private fun bindSafely(
        owner: LifecycleOwner,
        provider: ProcessCameraProvider,
        onError: (Throwable) -> Unit
    ) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        try {
            provider.unbindAll()
            val selector = if (isUltraWide) {
                provider.findWidestBackLens() ?: lens.selector
            } else {
                lens.selector
            }
            camera = provider.bindToLifecycle(owner, selector, preview, capture)
            imageCapture = capture
            setTorch(torchEnabled)
        } catch (e: Exception) {
            // The requested lens may not exist on this device - fall back to the other one.
            val fallback = lens.flipped()
            if (provider.hasCameraSafely(fallback.selector)) {
                lens = fallback
                try {
                    camera = provider.bindToLifecycle(owner, lens.selector, preview, capture)
                    imageCapture = capture
                    setTorch(torchEnabled)
                    return
                } catch (fallbackError: Exception) {
                    onError(fallbackError)
                    return
                }
            }
            onError(e)
        }
    }

    private fun ProcessCameraProvider.hasCameraSafely(selector: CameraSelector) =
        try {
            hasCamera(selector)
        } catch (e: Exception) {
            false
        }
}

/**
 * CameraX hands back a buffer in sensor orientation; rotate (and un-mirror the front lens)
 * so the bitmap matches what the user saw in the preview.
 */
private fun ImageProxy.toUprightBitmap(mirrored: Boolean): Bitmap {
    val source = toBitmap()
    val degrees = imageInfo.rotationDegrees
    if (degrees == 0 && !mirrored) return source
    val matrix = Matrix().apply {
        postRotate(degrees.toFloat())
        if (mirrored) postScale(-1F, 1F)
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        .also { if (it != source) source.recycle() }
}
