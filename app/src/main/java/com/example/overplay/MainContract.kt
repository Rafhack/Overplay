package com.example.overplay

import android.content.res.Configuration
import android.view.Surface
import com.example.overplay.MainViewState.TiltSensorData.TiltSensorState

data class MainViewState(
    val playerState: PlayerState,
    val tiltSensorData: TiltSensorData,
    val orientation: Int
) {

    data class PlayerState(
        val isLoading: Boolean
    )

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
            playerState = PlayerState(
                isLoading = true
            ),
            tiltSensorData = TiltSensorData(
                isInitialized = false,
                xAxisData = TiltSensorData.AxisData(),
                yAxisData = TiltSensorData.AxisData(),
                zAxisData = TiltSensorData.AxisData(),
                tiltState = TiltSensorState.IDLE
            ),
            orientation = Surface.ROTATION_0
        )
    }
}

@Suppress("ArrayInDataClass")
sealed class MainUserAction {
    data class ViewScreen(val orientation: Int) : MainUserAction()

    data class OrientationChanged(val orientation: Int): MainUserAction()
    data object ResetViewpointPressed : MainUserAction()
    data class SensorChanged(val sensorValues: FloatArray) : MainUserAction()
}