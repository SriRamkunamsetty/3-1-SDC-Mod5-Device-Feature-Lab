package com.example.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class SensorData(
    val compassHeading: Int = 0,     // 0 to 359 degrees
    val pitch: Float = 0f,           // Tilt front/back
    val roll: Float = 0f,            // Tilt left/right
    val totalGForce: Float = 1.0f,   // Total acceleration Gs
    val isPhysicalSensor: Boolean = false
)

class DeviceSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private var gravityValues = FloatArray(3)
    private var geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    fun startListening() {
        var registered = false
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            registered = true
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            registered = true
        }

        // If no hardware sensors are active, start a safe mock simulator to demonstrate functionality
        if (!registered) {
            _sensorData.value = _sensorData.value.copy(isPhysicalSensor = false)
        } else {
            _sensorData.value = _sensorData.value.copy(isPhysicalSensor = true)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    fun simulateSensorChange(headingDelta: Int, tiltDelta: Float) {
        val current = _sensorData.value
        if (!current.isPhysicalSensor) {
            val newHeading = (current.compassHeading + headingDelta + 360) % 360
            val newPitch = (current.pitch + tiltDelta).coerceIn(-90f, 90f)
            val newRoll = (current.roll - tiltDelta).coerceIn(-90f, 90f)
            _sensorData.value = SensorData(
                compassHeading = newHeading,
                pitch = newPitch,
                roll = newRoll,
                totalGForce = 1.0f + (newPitch / 90f) * 0.1f,
                isPhysicalSensor = false
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, event.values.size)
            hasGravity = true

            // Calculate total G-Force
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForceSum = kotlin.math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
            _sensorData.value = _sensorData.value.copy(
                totalGForce = (gForceSum * 100).roundToInt() / 100f
            )
        }

        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, event.values.size)
            hasGeomagnetic = true
        }

        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)

                // Azimuth (heading) is orientation[0] in radians, converted to degrees
                var heading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (heading < 0) {
                    heading += 360f
                }

                // Pitch is orientation[1] (tilt back/forward)
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()

                // Roll is orientation[2] (tilt left/right)
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                _sensorData.value = SensorData(
                    compassHeading = heading.roundToInt(),
                    pitch = pitch,
                    roll = roll,
                    totalGForce = _sensorData.value.totalGForce,
                    isPhysicalSensor = true
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
