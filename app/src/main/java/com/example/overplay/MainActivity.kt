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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.overplay.MainViewState.TiltSensorData.TiltSensorState
import com.example.overplay.databinding.ActivityMainBinding
import com.example.overplay.viewModel.MainViewModel
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : AppCompatActivity() {

    // region Global variables
    private lateinit var binding: ActivityMainBinding
    private lateinit var shakeDetector: ShakeDetector

    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val exoPlayer by lazy { createPlayer() }
    private val viewModel: MainViewModel by viewModels()
    // endregion

    // region Activity overridden methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rotation = ContextCompat.getDisplayOrDefault(this).rotation
        viewModel.dispatch(MainUserAction.ViewScreen(rotation))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.stateFlow.collect(::renderViewState) }
                launch { viewModel.sideEffect.collect(::handleSideEffect) }
            }
        }

        initGyroscope()
        initShakeDetector()
        initPlayer()
        setupListeners()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector.stop()
        exoPlayer.release()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rotation = ContextCompat.getDisplayOrDefault(this).rotation
        viewModel.dispatch(MainUserAction.OrientationChanged(rotation))
    }
    // endregion

    // region Private methods
    private fun handleSideEffect(sideEffect: MainSideEffect) {
        when (sideEffect) {
            is MainSideEffect.LoadVideo -> preparePlayer(sideEffect.videoUrl)
            MainSideEffect.PauseVideo -> exoPlayer.pause()
            MainSideEffect.PlayVideo -> exoPlayer.play()
        }
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

    // region Player methods
    private fun createPlayer() = ExoPlayer.Builder(this)
        .setSeekBackIncrementMs(PLAYER_SEEK_INTERVAL)
        .setSeekForwardIncrementMs(PLAYER_SEEK_INTERVAL)
        .build()

    private fun volumeUp() = with(binding) {
        hideIndicators()
        frameVolumeUpIndicator.isVisible = true
        exoPlayer.volume += PLAYER_VOLUME_INTERVAL
    }

    private fun volumeDown() = with(binding) {
        hideIndicators()
        frameVolumeDownIndicator.isVisible = true
        exoPlayer.volume -= PLAYER_VOLUME_INTERVAL
    }

    private fun rewind() = with(binding) {
        hideIndicators()
        frameRewindIndicator.isVisible = true
        exoPlayer.seekBack()
        playerView.showController()
    }

    private fun fastForward() = with(binding) {
        hideIndicators()
        frameFastForwardIndicator.isVisible = true
        exoPlayer.seekForward()
        playerView.showController()
    }

    private fun preparePlayer(videoUrl: String) {
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    private fun initPlayer() = with(binding) {
        playerView.player = exoPlayer
        playerView.setShowPreviousButton(false)
        playerView.setShowNextButton(false)
        playerView.setShowRewindButton(false)
        playerView.setShowFastForwardButton(false)
        playerView.controllerShowTimeoutMs = PLAYER_CONTROLS_TIMEOUT
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                viewModel.dispatch(MainUserAction.PlaybackStateChanged(playbackState))
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                viewModel.dispatch(MainUserAction.IsPlayingChanged(isPlaying))
            }
        })
    }
    // endregion

    private fun setupListeners() = with(binding) {
        buttonSetViewpoint.setOnClickListener {
            viewModel.dispatch(MainUserAction.SetViewpointPressed)
        }
    }

    // region Sensor Listeners
    private fun initGyroscope() {
        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    event.timestamp
                    viewModel.dispatch(MainUserAction.SensorChanged(it.values))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    /**
     * Uses the [ShakeDetector] class from Square's library Seismic.
     * Slightly modified to return the timestamp for each shake, so
     * we can ignore shake events too close from one another
     * */
    private fun initShakeDetector() {
        shakeDetector = ShakeDetector(object : ShakeDetector.Listener {
            override fun hearShake(timestamp: Long) {
                viewModel.dispatch(MainUserAction.DeviceShaken(timestamp))
            }
        })
        shakeDetector.start(sensorManager, SensorManager.SENSOR_DELAY_GAME)
    }
    // endregion
    // endregion

    companion object {
        private const val PLAYER_VOLUME_INTERVAL = .05F
        private const val PLAYER_SEEK_INTERVAL = 200L
        private const val PLAYER_CONTROLS_TIMEOUT = 1000
    }
}