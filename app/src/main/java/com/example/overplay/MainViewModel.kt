package com.example.overplay

import android.hardware.SensorManager
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overplay.MainViewState.TiltSensorData
import com.example.overplay.MainViewState.TiltSensorData.TiltSensorState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _mainStateFlow = MutableStateFlow(MainViewState.INITIAL_STATE)
    val mainStateFlow get() = _mainStateFlow.asStateFlow()

    private val userAction = MutableSharedFlow<MainUserAction>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            userAction.asSharedFlow().collect(::handleUserAction)
        }
    }

    fun dispatch(userAction: MainUserAction) = this.userAction.tryEmit(userAction)

    private suspend fun handleUserAction(action: MainUserAction) = when (action) {
        is MainUserAction.ViewScreen -> changeOrientation(action.orientation)
        is MainUserAction.SensorChanged -> updateSensorData(action.sensorValues)
        is MainUserAction.ResetViewpointPressed -> resetViewPoint()
        is MainUserAction.OrientationChanged -> changeOrientation(action.orientation)
    }


    private suspend fun changeOrientation(orientation: Int) {
        resetViewPoint()
        updateState { copy(orientation = orientation) }
    }

    private suspend fun resetViewPoint() = updateState {
        copy(tiltSensorData = tiltSensorData.copy(isInitialized = false, tiltState = TiltSensorState.IDLE))
    }

    private suspend fun updateSensorData(sensorValues: FloatArray) = updateState {
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

            copy(
                tiltSensorData = tiltSensorData.copy(
                    isInitialized = true,
                    tiltState = state,
                    xAxisData = updatedXData,
                    yAxisData = updatedYData,
                    zAxisData = updatedZData
                )
            )
        }
    }

    private fun getAxisByOrientation(
        orientation: Int,
        xAxisData: TiltSensorData.AxisData,
        yAxisData: TiltSensorData.AxisData,
        zAxisData: TiltSensorData.AxisData,
    ): Pair<TiltSensorData.AxisData, TiltSensorData.AxisData> {
        return when (orientation) {
            Surface.ROTATION_0 -> xAxisData
            Surface.ROTATION_90 -> yAxisData.invert()
            Surface.ROTATION_180 -> xAxisData.invert()
            else -> yAxisData
        } to when (orientation) {
            Surface.ROTATION_0 -> yAxisData
            Surface.ROTATION_90 -> zAxisData
            Surface.ROTATION_180 -> yAxisData.invert()
            else -> zAxisData.invert()
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

    private fun TiltSensorData.AxisData.updateAxisData(sensorValue: Float, isInitialized: Boolean): TiltSensorData.AxisData {
        val origin = if (isInitialized) origin else sensorValue
        val offset = sensorValue - origin
        return copy(current = sensorValue, origin = origin, offset = offset)
    }

    private fun TiltSensorData.AxisData.invert(): TiltSensorData.AxisData {
        return copy(current = -current, origin = -origin, offset = -offset)
    }

    private suspend fun updateState(reduce: (MainViewState.() -> MainViewState)) {
        _mainStateFlow.value.let { _mainStateFlow.emit(reduce(it)) }
    }

    companion object {
        const val MIN_TILT_THRESHOLD = 20F
        const val MAX_TILT_THRESHOLD = 60F
    }

}