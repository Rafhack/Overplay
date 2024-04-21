package com.example.overplay

import com.example.overplay.MainViewState.TiltSensorData.TiltSensorState

data class MainViewState(
    val playerState: PlayerState,
    val tiltSensorData: TiltSensorData
) {

    data class PlayerState(
        val isLoading: Boolean
    )

    data class TiltSensorData(
        val xAxisData: AxisData,
        val zAxisData: AxisData,
        val tiltState: TiltSensorState
    ) {
        enum class TiltSensorState {
            TILTING_LEFT,
            TILTING_RIGHT,
            TILTING_UP,
            TILTING_DOWN,
            IDLE
        }

        data class AxisData(
            val current: Double = 0.0,
            val origin: Double = 0.0,
            val offset: Double = 0.0
        )
    }

    companion object {
        val INITIAL_STATE = MainViewState(
            playerState = PlayerState(
                isLoading = true
            ),
            tiltSensorData = TiltSensorData(
                xAxisData = TiltSensorData.AxisData(),
                zAxisData = TiltSensorData.AxisData(),
                tiltState = TiltSensorState.IDLE
            )
        )
    }
}

sealed class MainUserAction {
    data object ViewScreen : MainUserAction()
}