package com.example.overplay.ui.main.viewModel

import android.view.Surface
import androidx.media3.common.Player
import com.example.overplay.ui.main.viewModel.MainViewState.TiltSensorData.TiltSensorState

data class MainViewState(
    val tiltSensorData: TiltSensorData,
    val orientation: Int,
    val playbackState: Int,
    val isVideoPlaying: Boolean,
    val lastShakeEventTimestamp: Long
) {
    data class TiltSensorData(
        val isInitialized: Boolean,
        val xAxisData: AxisData,
        val yAxisData: AxisData,
        val zAxisData: AxisData,
        val tiltState: TiltSensorState,
    ) {
        enum class TiltSensorState {
            TILTING_LEFT,
            TILTING_RIGHT,
            TILTING_UP,
            TILTING_DOWN,
            IDLE
        }

        data class AxisData(
            val current: Float = 0F,
            val origin: Float = 0F,
            val offset: Float = 0F
        )
    }

    companion object {
        val INITIAL_STATE = MainViewState(
            playbackState = Player.STATE_IDLE,
            isVideoPlaying = false,
            lastShakeEventTimestamp = 0L,
            orientation = Surface.ROTATION_0,
            tiltSensorData = TiltSensorData(
                isInitialized = false,
                xAxisData = TiltSensorData.AxisData(),
                yAxisData = TiltSensorData.AxisData(),
                zAxisData = TiltSensorData.AxisData(),
                tiltState = TiltSensorState.IDLE
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
}

sealed class MainSideEffect {
    data class LoadVideo(val videoUrl: String) : MainSideEffect()
    data object PlayVideo : MainSideEffect()
    data object PauseVideo : MainSideEffect()
}