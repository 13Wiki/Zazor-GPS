package com.gps.zazor.utils.location

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Which way the back camera is pointing, in degrees clockwise from north.
 *
 * A photograph says where the person stood; it does not say which way they were looking, and for
 * a wide frame of a forest edge that is most of what the receiver needs in order to stand in the
 * same place and see the same thing. The direction is written onto the panorama beside the
 * coordinates.
 *
 * The sensor reports magnetic north. Magnetic north is not the north on a map - the difference
 * reaches tens of degrees at high latitudes - so the declination for the current position is
 * added and the reported bearing is a true one. Without a position the raw magnetic reading is
 * the honest best, and it is what gets used.
 */
class CompassProvider(private val context: Context) {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    /** False on a phone with no compass hardware; the caller then simply has no direction. */
    val isAvailable: Boolean
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    /**
     * Emits the bearing while collected, at the sensor's UI rate - fast enough to follow a turn,
     * slow enough not to wake the CPU for every twitch.
     *
     * @param location used for the declination; pass the freshest fix there is.
     */
    fun bearings(location: () -> Location?): Flow<Float> = callbackFlow {
        val manager = sensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) {
            close()
            return@callbackFlow
        }
        val rotation = FloatArray(ROTATION_MATRIX_SIZE)
        val camera = FloatArray(ROTATION_MATRIX_SIZE)
        val orientation = FloatArray(ORIENTATION_SIZE)
        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                // The device's own axes point out of the screen; the camera looks the other way,
                // so the frame is remapped before the angle is read. Without this the bearing is
                // the direction the person is facing, not the direction they are photographing.
                SensorManager.remapCoordinateSystem(
                    rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, camera
                )
                SensorManager.getOrientation(camera, orientation)
                val magnetic = Math.toDegrees(orientation[0].toDouble()).toFloat()
                trySend(normalize(magnetic + declination(location())))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }

    private fun declination(location: Location?): Float {
        val fix = location ?: return 0F
        return GeomagneticField(
            fix.latitude.toFloat(),
            fix.longitude.toFloat(),
            fix.altitude.toFloat(),
            fix.time
        ).declination
    }

    /** Into 0..360, which is how a bearing is written down and read out loud. */
    private fun normalize(degrees: Float): Float = (degrees % FULL_TURN + FULL_TURN) % FULL_TURN

    private companion object {

        const val ROTATION_MATRIX_SIZE = 9
        const val ORIENTATION_SIZE = 3
        const val FULL_TURN = 360F
    }
}
