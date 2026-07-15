package com.Crescent.DhikrCounter.ui.components.catalog.utils

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

@Stable
class UISensor(context: android.content.Context) {
    private val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var offset by mutableStateOf(Offset.Zero)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                offset = Offset(-event.values[0], event.values[1])
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    fun start() {
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}

@Composable
fun rememberUISensor(): UISensor {
    val context = LocalContext.current
    val sensor = remember { UISensor(context) }
    DisposableEffect(sensor) {
        sensor.start()
        onDispose { sensor.stop() }
    }
    return sensor
}
