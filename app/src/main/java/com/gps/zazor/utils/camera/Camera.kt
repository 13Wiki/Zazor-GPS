package com.gps.zazor.utils.camera

import androidx.camera.core.CameraSelector

/**
 * Which physical camera the preview is bound to.
 */
enum class Camera(val selector: CameraSelector) {

    BACK(CameraSelector.DEFAULT_BACK_CAMERA),
    FRONT(CameraSelector.DEFAULT_FRONT_CAMERA);

    fun flipped() = if (this == BACK) FRONT else BACK
}
