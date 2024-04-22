package com.example.overplay

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.overplay.MainViewState.TiltSensorData.TiltSensorState
import com.example.overplay.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val exoPlayer by lazy { ExoPlayer.Builder(this).build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.dispatch(MainUserAction.ViewScreen(resources.configuration.orientation))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainStateFlow.collect(::renderViewState)
            }
        }

        initAccelerometer()
        setupListeners()
        initPlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rotation = ContextCompat.getDisplayOrDefault(this).rotation
        viewModel.dispatch(MainUserAction.OrientationChanged(rotation))
    }

    private fun renderViewState(viewState: MainViewState) {
        handleTiltState(viewState.tiltSensorData.tiltState)
    }

    private fun handleTiltState(state: TiltSensorState) = when (state) {
        TiltSensorState.TILTING_LEFT -> rewind()
        TiltSensorState.TILTING_RIGHT -> fastForward()
        TiltSensorState.TILTING_UP -> volumeUp()
        TiltSensorState.TILTING_DOWN -> volumeDown()
        TiltSensorState.IDLE -> hideIndicators()
    }

    private fun hideIndicators() = with(binding) {
        frameRewindIndicator.isVisible = false
        frameFastForwardIndicator.isVisible = false
        frameVolumeUpIndicator.isVisible = false
        frameVolumeDownIndicator.isVisible = false
    }

    private fun volumeUp() = with(binding) {
        hideIndicators()
        frameVolumeUpIndicator.isVisible = true
    }

    private fun volumeDown() = with(binding) {
        hideIndicators()
        frameVolumeDownIndicator.isVisible = true
    }


    private fun rewind() = with(binding) {
        hideIndicators()
        frameRewindIndicator.isVisible = true
    }

    private fun fastForward() = with(binding) {
        hideIndicators()
        frameFastForwardIndicator.isVisible = true
    }

    private fun setupListeners() = with(binding) {
        buttonResetViewpoint.setOnClickListener {
            viewModel.dispatch(MainUserAction.ResetViewpointPressed)
        }
    }

    private fun initPlayer() = with(binding) {
        val mediaItem = MediaItem.fromUri(VIDEO_URL)
        playerView.player = exoPlayer
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
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

    companion object {
        private const val VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
    }
}