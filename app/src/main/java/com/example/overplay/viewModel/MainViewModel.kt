package com.example.overplay.viewModel

import androidx.media3.common.Player
import com.example.overplay.MainSideEffect
import com.example.overplay.MainUserAction
import com.example.overplay.MainViewState
import com.example.overplay.MainViewState.TiltSensorData.TiltSensorState
import com.example.overplay.SensorUseCase

class MainViewModel : BaseViewModel<MainUserAction, MainSideEffect, MainViewState>(
    MainViewState.INITIAL_STATE
) {

    private val sensorUseCase by lazy { SensorUseCase() }

    override suspend fun handleUserAction(action: MainUserAction) = when (action) {
        is MainUserAction.ViewScreen -> startup(action.orientation)
        is MainUserAction.SensorChanged -> updateSensorData(action.sensorValues)
        is MainUserAction.ResetViewpointPressed -> resetViewPoint()
        is MainUserAction.OrientationChanged -> changeOrientation(action.orientation)
        is MainUserAction.DeviceShaken -> handleDeviceShakeEvent(action.timestamp)
        is MainUserAction.PlaybackStateChanged -> updatePlaybackState(action.playbackState)
        is MainUserAction.IsPlayingChanged -> updatePlayingState(action.isPlaying)
    }

    private suspend fun startup(orientation: Int) {
        changeOrientation(orientation)
        emitSideEffect(MainSideEffect.LoadVideo(VIDEO_URL))
    }

    private suspend fun updatePlaybackState(playbackState: Int) {
        updateState { copy(playbackState = playbackState) }
    }

    private suspend fun updatePlayingState(isPlaying: Boolean) {
        updateState { copy(isVideoPlaying = isPlaying) }
    }

    private suspend fun handleDeviceShakeEvent(currentTimestamp: Long) = with(stateFlow.value) {
        val deltaTime = currentTimestamp - stateFlow.value.lastShakeEventTimestamp
        updateState { copy(lastShakeEventTimestamp = currentTimestamp) }
        if (deltaTime < MIN_SHAKE_INTERVAL || playbackState != Player.STATE_READY) return
        if (isVideoPlaying) {
            emitSideEffect(MainSideEffect.PauseVideo)
        } else {
            emitSideEffect(MainSideEffect.PlayVideo)
        }
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

    companion object {
        private const val MIN_SHAKE_INTERVAL = 1000
        private const val VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
    }

}