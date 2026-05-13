package com.sameerasw.essentials.services

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.sameerasw.essentials.data.repository.SettingsRepository

class LiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null
        private lateinit var repository: SettingsRepository
        private val executor = android.os.Handler(android.os.Looper.getMainLooper())
        private val keyguardManager by lazy { getSystemService(KEYGUARD_SERVICE) as KeyguardManager }

        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                SettingsRepository.KEY_LIVE_WALLPAPER_SELECTED_VIDEO -> loadSelectedVideo()
                SettingsRepository.KEY_LIVE_WALLPAPER_PLAYBACK_TRIGGER -> {
                    // Update current playback state if screen is on
                    if (isVisible && !keyguardManager.isKeyguardLocked) {
                        exoPlayer?.play()
                    } else if (isVisible && repository.getLiveWallpaperPlaybackTrigger() == SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON) {
                        exoPlayer?.play()
                    }
                }
            }
        }

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> exoPlayer?.play()
                    Intent.ACTION_SCREEN_OFF -> {
                        executor.removeCallbacksAndMessages(null)
                        executor.postDelayed({
                            exoPlayer?.pause()
                            exoPlayer?.seekTo(0)
                        }, 500)
                    }

                    Intent.ACTION_SCREEN_ON -> {
                        val shouldPlay = isPreview ||
                                !keyguardManager.isKeyguardLocked ||
                                repository.getLiveWallpaperPlaybackTrigger() == SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON

                        if (shouldPlay) {
                            executor.removeCallbacksAndMessages(null)
                            exoPlayer?.play()
                        } else {
                            executor.removeCallbacksAndMessages(null)
                            executor.postDelayed({
                                exoPlayer?.pause()
                                exoPlayer?.seekTo(0)
                            }, 500)
                        }
                    }
                }
            }
        }

        @OptIn(UnstableApi::class)
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            repository = SettingsRepository(applicationContext)

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(receiver, filter)

            repository.registerOnSharedPreferenceChangeListener(prefsListener)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                val shouldPlay = isPreview ||
                        !keyguardManager.isKeyguardLocked ||
                        repository.getLiveWallpaperPlaybackTrigger() == SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON
                if (shouldPlay) exoPlayer?.play()
            } else {
                exoPlayer?.pause()
            }
        }

        @OptIn(UnstableApi::class)
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            val renderersFactory = DefaultRenderersFactory(applicationContext)
            val player = ExoPlayer.Builder(applicationContext, renderersFactory).build()
            exoPlayer = player

            player.apply {
                setVideoSurfaceHolder(holder)
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                loadSelectedVideo()
                val shouldPlay = isPreview ||
                        !keyguardManager.isKeyguardLocked ||
                        repository.getLiveWallpaperPlaybackTrigger() == SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON
                playWhenReady = shouldPlay
            }
        }

        @OptIn(UnstableApi::class)
        private fun loadSelectedVideo() {
            val videoName = repository.getLiveWallpaperSelectedVideo()
            val resId = resources.getIdentifier(videoName, "raw", packageName)
            val mediaItem = if (resId != 0) {
                MediaItem.fromUri("android.resource://$packageName/$resId")
            } else {
                try {
                    MediaItem.fromUri(Uri.parse(videoName))
                } catch (e: Exception) {
                    null
                }
            }

            mediaItem?.let {
                exoPlayer?.setMediaItem(it)
                exoPlayer?.prepare()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, f, w, h)
            exoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }

        override fun onDestroy() {
            super.onDestroy()
            executor.removeCallbacksAndMessages(null)
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
            }
            repository.unregisterOnSharedPreferenceChangeListener(prefsListener)
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
