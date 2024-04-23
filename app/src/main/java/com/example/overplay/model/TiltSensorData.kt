package com.example.overplay.model

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