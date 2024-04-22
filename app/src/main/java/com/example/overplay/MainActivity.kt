package com.example.overplay

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.overplay.MainViewState.TiltSensorData.TiltSensorState
import com.example.overplay.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initAccelerometer()

        viewModel.dispatch(MainUserAction.ViewScreen)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainStateFlow.collect(::renderViewState)
            }
        }
    }

    private fun renderViewState(viewState: MainViewState) {
        handleTiltState(viewState.tiltSensorData.tiltState)
        Log.d("Sensor", "X: ${viewState.tiltSensorData.xAxisData.offset}, Z: ${viewState.tiltSensorData.zAxisData.offset}")
    }

    private fun handleTiltState(state: TiltSensorState) = when (state) {
        TiltSensorState.TILTING_LEFT -> rewind()
        TiltSensorState.TILTING_RIGHT -> fastForward()
        TiltSensorState.TILTING_UP -> TODO()
        TiltSensorState.TILTING_DOWN -> TODO()
        TiltSensorState.IDLE -> hideIndicators()
    }

    private fun hideIndicators() {
        binding.frameRewindIndicator.isVisible = false
        binding.frameFastForwardIndicator.isVisible = false
    }

    private fun rewind() {
        binding.frameRewindIndicator.isVisible = true
        binding.frameFastForwardIndicator.isVisible = false
    }

    private fun fastForward() {
        binding.frameFastForwardIndicator.isVisible = true
        binding.frameRewindIndicator.isVisible = false
    }

    private fun initAccelerometer() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    viewModel.dispatch(MainUserAction.SensorChanged(it.values))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }, sensor, SensorManager.SENSOR_DELAY_UI)
    }
}