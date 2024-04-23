package com.example.overplay.ui.main.viewModel

import android.location.Location
import android.view.Surface
import androidx.media3.common.Player
import com.example.overplay.model.TiltSensorData

data class MainViewState(
    val tiltSensorData: TiltSensorData,
    val orientation: Int,
    val playbackState: Int,
    val isVideoPlaying: Boolean,
    val lastShakeEventTimestamp: Long,
    val isLocationInitialized: Boolean,
    val lastLocation: Location?,
    val lastLocationDistance: Float,
    val isDebugPanelVisible: Boolean
) {
    companion object {
        val INITIAL_STATE = MainViewState(
            playbackState = Player.STATE_IDLE,
            isVideoPlaying = false,
            lastShakeEventTimestamp = 0L,
            isLocationInitialized = false,
            orientation = Surface.ROTATION_0,
            lastLocation = null,
            lastLocationDistance = 0F,
            isDebugPanelVisible = false,
            tiltSensorData = TiltSensorData(
                isInitialized = false,
                xAxisData = TiltSensorData.AxisData(),
                yAxisData = TiltSensorData.AxisData(),
                zAxisData = TiltSensorData.AxisData(),
                tiltState = TiltSensorData.TiltSensorState.IDLE
            )
        )
    }
}

sealed class MainUserAction {
    data class ViewScreen(val orientation: Int) : MainUserAction()

    data class OrientationChanged(val orientation: Int) : MainUserAction()

    @Suppress("ArrayInDataClass")
    data class SensorChanged(val sensorValues: FloatArray) : MainUserAction()
    data class DeviceShaken(val timestamp: Long) : MainUserAction()

    data class PlaybackStateChanged(val playbackState: Int) : MainUserAction()

    data class IsPlayingChanged(val isPlaying: Boolean) : MainUserAction()

    data object SetViewpointPressed : MainUserAction()

    data object DebugPanelPressed : MainUserAction()

    data object ActivityPaused : MainUserAction()

    data class ActivityResumed(val hasLocationPermission: Boolean) : MainUserAction()

    data object OnLocationPermissionGranted : MainUserAction()

    data class LocationUpdated(val location: Location?) : MainUserAction()
}

sealed class MainSideEffect {
    data class LoadVideo(val videoUrl: String) : MainSideEffect()
    data object PlayVideo : MainSideEffect()
    data object PauseVideo : MainSideEffect()

    data object StartLocationUpdates : MainSideEffect()
    data object PauseLocationUpdates : MainSideEffect()
}