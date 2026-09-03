package com.gps.zazor.utils.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

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
        bindSafely(owner, provider) { throw it }
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
            camera = provider.bindToLifecycle(owner, lens.selector, preview, capture)
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
