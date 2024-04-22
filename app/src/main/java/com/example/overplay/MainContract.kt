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
        val isInitialized: Boolean,
        val xAxisData: AxisData,
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
        ) {
            override fun toString(): String {
                return "Current: $current, Origin: $origin, Offset: $offset"
            }
        }
    }

    companion object {
        val INITIAL_STATE = MainViewState(
            playerState = PlayerState(
                isLoading = true
            ),
            tiltSensorData = TiltSensorData(
                isInitialized = false,
                xAxisData = TiltSensorData.AxisData(),
                zAxisData = TiltSensorData.AxisData(),
                tiltState = TiltSensorState.IDLE
            )
        )
    }
}

sealed class MainUserAction {
    data object ViewScreen : MainUserAction()
    data class SensorChanged(val sensorValues: FloatArray) : MainUserAction() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SensorChanged

            return sensorValues.contentEquals(other.sensorValues)
        }

        override fun hashCode(): Int {
            return sensorValues.contentHashCode()
        }
    }
}