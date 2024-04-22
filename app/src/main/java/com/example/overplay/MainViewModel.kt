package com.example.overplay

import android.hardware.SensorManager
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
        is MainUserAction.ViewScreen -> Unit
        is MainUserAction.SensorChanged -> updateSensorData(action.sensorValues)
        is MainUserAction.ResetViewpointPressed -> resetViewPoint()
    }

    private suspend fun resetViewPoint() = updateState {
        copy(tiltSensorData = tiltSensorData.copy(isInitialized = false, tiltState = TiltSensorState.IDLE))
    }

    private suspend fun updateSensorData(sensorValues: FloatArray) = updateState {
        with(tiltSensorData) {

            val rotationValues = getRotation(sensorValues)

            val updatedXData = xAxisData.updateAxisData(rotationValues.component1(), isInitialized)
            val updatedYData = yAxisData.updateAxisData(rotationValues.component2(), isInitialized)

            var state = when (updatedYData.offset) {
                in -180F..-TILT_THRESHOLD -> TiltSensorState.TILTING_DOWN
                in TILT_THRESHOLD..180F -> TiltSensorState.TILTING_UP
                else -> TiltSensorState.IDLE
            }
            state = if (state == TiltSensorState.IDLE) when (updatedXData.offset) {
                in -180F..-TILT_THRESHOLD -> TiltSensorState.TILTING_LEFT
                in TILT_THRESHOLD..180F -> TiltSensorState.TILTING_RIGHT
                else -> TiltSensorState.IDLE
            } else state

            copy(
                tiltSensorData = tiltSensorData.copy(
                    isInitialized = true,
                    tiltState = state,
                    xAxisData = updatedXData,
                    yAxisData = updatedYData
                )
            )
        }
    }

    private fun getRotation(sensorValues: FloatArray): FloatArray {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorValues)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        return floatArrayOf(
            Math.toDegrees(orientationAngles[0].toDouble()).toFloat(),
            Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
            Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        )
    }

    private fun TiltSensorData.AxisData.updateAxisData(sensorValue: Float, isInitialized: Boolean): TiltSensorData.AxisData {
        val origin = if (isInitialized) origin else sensorValue
        val offset = sensorValue - origin
        return copy(current = sensorValue, origin = origin, offset = offset)
    }

    private suspend fun updateState(reduce: (MainViewState.() -> MainViewState)) {
        _mainStateFlow.value.let { _mainStateFlow.emit(reduce(it)) }
    }

    companion object {
        const val TILT_THRESHOLD = 20F
    }

}