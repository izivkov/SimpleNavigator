package org.avmedia.simplenavigator

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.avmedia.simplenavigator.EventProcessor.onNext


data class StepCounter(val sensorManager: SensorManager) : SensorEventListener {

    private var totalSteps = 0
    private var isPaused = false

    init {
        val stepsSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepsSensor != null) {
            sensorManager.registerListener(this, stepsSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (isPaused) {
            return
        }

        var steps = 0
        if (p0 != null) {
            if (p0.values != null && p0.values.size > 0) {
                steps = p0.values.get(0).toInt()
            }
        }
        val event: EventProcessor.ProgressEvents =
            EventProcessor.ProgressEvents.StepsChangeEvent

        totalSteps += steps
        event.payload = totalSteps.toString()

        onNext(event)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    fun clear() {
        totalSteps = 0
    }

    fun togglePause() {
        if (isPaused) {
            resume()
        } else {
            pause()
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }
}


