package com.example.overplay.domain

import android.hardware.SensorManager
import android.view.Surface
import com.example.overplay.model.TiltSensorData
import com.example.overplay.model.TiltSensorData.TiltSensorState

class SensorUseCase {

    fun updateSensorData(
        sensorValues: FloatArray,
        orientation: Int,
        tiltSensorData: TiltSensorData
    ): TiltSensorData {
        with(tiltSensorData) {

            val rotationValues = getRotation(sensorValues)

            val updatedXData = xAxisData.updateAxisData(rotationValues.component1(), isInitialized)
            val updatedYData = yAxisData.updateAxisData(rotationValues.component2(), isInitialized)
            val updatedZData = zAxisData.updateAxisData(rotationValues.component3(), isInitialized)

            val referenceAxis = getAxisByOrientation(orientation, xAxisData, yAxisData, zAxisData)
            val tiltAxis = referenceAxis.first
            val yawAxis = referenceAxis.second

            var state = when (yawAxis.offset) {
                in -MAX_TILT_THRESHOLD..-MIN_TILT_THRESHOLD -> TiltSensorState.TILTING_DOWN
                in MIN_TILT_THRESHOLD..MAX_TILT_THRESHOLD -> TiltSensorState.TILTING_UP
                else -> TiltSensorState.IDLE
            }
            state = if (state == TiltSensorState.IDLE) when (tiltAxis.offset) {
                in -MAX_TILT_THRESHOLD..-MIN_TILT_THRESHOLD -> TiltSensorState.TILTING_LEFT
                in MIN_TILT_THRESHOLD..MAX_TILT_THRESHOLD -> TiltSensorState.TILTING_RIGHT
                else -> TiltSensorState.IDLE
            } else state


            return tiltSensorData.copy(
                isInitialized = true,
                tiltState = state,
                xAxisData = updatedXData,
                yAxisData = updatedYData,
                zAxisData = updatedZData
            )

        }
    }

    private fun getRotation(sensorValues: FloatArray): FloatArray {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorValues)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        return floatArrayOf(
            Math.toDegrees(orientationAngles.component1().toDouble()).toFloat(),
            Math.toDegrees(orientationAngles.component2().toDouble()).toFloat(),
            Math.toDegrees(orientationAngles.component3().toDouble()).toFloat()
        )
    }

    private fun TiltSensorData.AxisData.updateAxisData(
        sensorValue: Float,
        isInitialized: Boolean
    ): TiltSensorData.AxisData {
        val origin = if (isInitialized) origin else sensorValue
        val offset = sensorValue - origin
        return copy(current = sensorValue, origin = origin, offset = offset)
    }

    private fun getAxisByOrientation(
        orientation: Int,
        xAxisData: TiltSensorData.AxisData,
        yAxisData: TiltSensorData.AxisData,
        zAxisData: TiltSensorData.AxisData,
    ): Pair<TiltSensorData.AxisData, TiltSensorData.AxisData> {
        return when (orientation) {
            Surface.ROTATION_0 -> xAxisData              // Portrait
            Surface.ROTATION_90 -> yAxisData.invert()    // Landscape
            Surface.ROTATION_180 -> xAxisData.invert()   // Reverse Portrait
            else -> yAxisData                            // Reverse Landscape
        } to when (orientation) {
            Surface.ROTATION_0 -> yAxisData
            Surface.ROTATION_90 -> zAxisData
            Surface.ROTATION_180 -> yAxisData.invert()
            else -> zAxisData.invert()
        }
    }

    private fun TiltSensorData.AxisData.invert(): TiltSensorData.AxisData {
        return copy(current = -current, origin = -origin, offset = -offset)
    }

    companion object {
        private const val MIN_TILT_THRESHOLD = 20F
        private const val MAX_TILT_THRESHOLD = 60F
    }

}