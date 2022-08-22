package com.gps.zazor.utils

import android.graphics.Bitmap
import com.dermandar.dmd_lib.CallbackInterfaceShooter
import com.dermandar.dmd_lib.DMD_Capture
import java.util.HashMap

open class SimpleShooterCallback : CallbackInterfaceShooter {
    override fun onExposureChanged(p0: DMD_Capture.ExposureMode?) = Unit

    override fun preparingToShoot() = Unit

    override fun canceledPreparingToShoot() = Unit

    override fun takingPhoto() = Unit

    override fun shotTakenPreviewReady(p0: Bitmap?) = Unit

    override fun photoTaken() = Unit

    override fun stitchingCompleted(p0: HashMap<String, Any>?) = Unit

    override fun shootingCompleted(p0: Boolean) = Unit

    override fun onFinishGeneratingEqui() = Unit

    override fun onFinishClear() = Unit

    override fun onFinishRelease() = Unit

    override fun deviceVerticalityChanged(p0: Int) = Unit

    override fun compassEvent(p0: HashMap<String, Any>?) = Unit

    override fun onDirectionUpdated(p0: Float) = Unit

    override fun onCameraStarted() = Unit

    override fun onCameraStopped() = Unit

    override fun onRotatorConnected() = Unit

    override fun onRotatorDisconnected() = Unit

    override fun onStartedRotating() = Unit

    override fun onFinishedRotating() = Unit

}