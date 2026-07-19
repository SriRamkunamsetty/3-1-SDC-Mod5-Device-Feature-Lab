package com.example.hardware

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class HardwareController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private var isFlashOn = false

    fun toggleFlashlight(on: Boolean): Boolean {
        try {
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isNotEmpty()) {
                val cameraId = cameraIdList[0]
                cameraManager.setTorchMode(cameraId, on)
                isFlashOn = on
                return true
            }
        } catch (e: Exception) {
            Log.e("HardwareController", "Flashlight execution failed: ${e.message}. Fallbacking to virtual state.")
        }
        // Fallback or simulation for emulators / environment sandbox
        isFlashOn = on
        return false
    }

    fun getBatteryPercentage(): Int {
        val pct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (pct <= 0) 100 else pct // default mock for emulators returning 0
    }

    fun isBatteryCharging(): Boolean {
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    @Suppress("DEPRECATION")
    fun triggerVibration(durationMs: Long = 100) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            Log.e("HardwareController", "Vibration failed: ${e.message}")
        }
    }
}
