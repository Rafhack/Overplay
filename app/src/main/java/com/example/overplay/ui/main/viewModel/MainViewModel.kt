package com.example.overplay.ui.main.viewModel

import android.util.Log
import androidx.media3.common.Player
import com.example.overplay.domain.SensorUseCase
import com.example.overplay.ui.main.viewModel.MainViewState.TiltSensorData.TiltSensorState

class MainViewModel : BaseViewModel<MainUserAction, MainSideEffect, MainViewState>(
    MainViewState.INITIAL_STATE
) {

    private val sensorUseCase by lazy { SensorUseCase() }

    override suspend fun handleUserAction(action: MainUserAction) = when (action) {
        is MainUserAction.ViewScreen -> startup(action.orientation)
        is MainUserAction.SensorChanged -> updateSensorData(action.sensorValues)
        is MainUserAction.SetViewpointPressed -> setViewPoint()
        is MainUserAction.OrientationChanged -> changeOrientation(action.orientation)
        is MainUserAction.DeviceShaken -> handleDeviceShakeEvent(action.timestamp)
        is MainUserAction.PlaybackStateChanged -> updatePlaybackState(action.playbackState)
        is MainUserAction.IsPlayingChanged -> updatePlayingState(action.isPlaying)
        is MainUserAction.ActivityPaused -> pauseVideoAndLocation()
        is MainUserAction.ActivityResumed -> resumeVideoAndLocation(action.hasLocationPermission)
        is MainUserAction.OnLocationPermissionGranted -> emitSideEffect(MainSideEffect.StartLocationUpdates)
    }

    private suspend fun startup(orientation: Int) {
        changeOrientation(orientation)
        emitSideEffect(MainSideEffect.LoadVideo(VIDEO_URL))
    }

    private suspend fun pauseVideoAndLocation() {
        if (stateFlow.value.isVideoPlaying) {
            Log.d("SideEffect", "Video IsPlaying")
            emitSideEffect(MainSideEffect.PauseVideo)
        }
        if (stateFlow.value.isLocationInitialized) {
            emitSideEffect(MainSideEffect.PauseLocationUpdates)
        }
    }

    private suspend fun resumeVideoAndLocation(hasLocationPermission: Boolean) {
        if (stateFlow.value.playbackState == Player.STATE_READY) {
            emitSideEffect(MainSideEffect.PlayVideo)
        }
        if (hasLocationPermission) {
            updateState { copy(isLocationInitialized = true) }
            emitSideEffect(MainSideEffect.StartLocationUpdates)
        }
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
        setViewPoint()
        updateState { copy(orientation = orientation) }
    }

    private suspend fun setViewPoint() = updateState {
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