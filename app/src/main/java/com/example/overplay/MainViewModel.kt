package com.example.overplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val sensorUseCase by lazy { SensorUseCase() }

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
        copy(
            tiltSensorData = sensorUseCase.updateSensorData(sensorValues, orientation, tiltSensorData)
        )
    }

    private suspend fun updateState(reduce: (MainViewState.() -> MainViewState)) {
        _mainStateFlow.value.let { _mainStateFlow.emit(reduce(it)) }
    }

    companion object {

    }

}