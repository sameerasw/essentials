/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: LiveWallpaperService.kt
 * Description: Background service component for LiveWallpaperService.kt.
 */

package com.sameerasw.essentials.services

import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.animation.DecelerateInterpolator
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.data.repository.WallpaperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LiveWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null
        private lateinit var repository: SettingsRepository
        private val executor = Handler(Looper.getMainLooper())
        private val keyguardManager by lazy { getSystemService(KEYGUARD_SERVICE) as KeyguardManager }
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        private var surfaceReady = false
        private var imageMode = false
        private var imageLoadJob: Job? = null
        private var dailyBitmap: Bitmap? = null
        private var blurCacheKey: String? = null
        private var blurNode: RenderNode? = null
        private var blurFallback: Bitmap? = null
        private var blurProgress = 0f
        private var blurAnimator: ValueAnimator? = null

        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val maskPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        private val scrimPaint = Paint()

        private val livePrefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    SettingsRepository.KEY_LIVE_WALLPAPER_SELECTED_VIDEO -> applyMode()
                    SettingsRepository.KEY_LIVE_WALLPAPER_PLAYBACK_TRIGGER -> {
                        if (isVisible && !keyguardManager.isKeyguardLocked) {
                            exoPlayer?.play()
                        } else if (isVisible &&
                            repository.getLiveWallpaperPlaybackTrigger() == SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON
                        ) {
                            exoPlayer?.play()
                        }
                    }
                    SettingsRepository.KEY_LIVE_WALLPAPER_PROGRESSIVE_BLUR,
                    SettingsRepository.KEY_LIVE_WALLPAPER_PROGRESSIVE_BLUR_HEIGHT,
                    -> {
                        blurProgress = blurTarget()
                        drawImage()
                    }
                }
            }

        private val prefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL_MOBILE && imageMode) {
                    loadDailyImage()
                }
            }

        private val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    when (intent?.action) {
                        Intent.ACTION_USER_PRESENT -> {
                            exoPlayer?.play()
                            // keyguard can still report locked while USER_PRESENT is delivered
                            animateBlurTo(if (blurAllowed()) 1f else 0f)
                        }

                        Intent.ACTION_SCREEN_OFF -> {
                            setBlurImmediately(0f)
                            discardBlurNode()
                            executor.removeCallbacksAndMessages(null)
                            executor.postDelayed({
                                exoPlayer?.pause()
                                exoPlayer?.seekTo(0)
                            }, 500)
                        }

                        Intent.ACTION_SCREEN_ON -> {
                            if (keyguardManager.isKeyguardLocked) {
                                setBlurImmediately(0f)
                            } else {
                                animateBlurTo(blurTarget())
                            }

                            val shouldPlay =
                                isPreview ||
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

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            repository = SettingsRepository(applicationContext)
            imageMode = repository.isLiveWallpaperDailyImageSelected()

            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                }
            registerReceiver(receiver, filter)

            repository.registerOnSharedPreferenceChangeListener(prefsListener)
            repository.registerLiveWallpaperPrefsListener(livePrefsListener)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (imageMode) {
                if (visible) {
                    blurAnimator?.cancel()
                    blurProgress = blurTarget()
                    drawImage()
                }
                return
            }
            if (visible) {
                val shouldPlay =
                    isPreview ||
                        !keyguardManager.isKeyguardLocked ||
                        repository.getLiveWallpaperPlaybackTrigger() == SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON
                if (shouldPlay) exoPlayer?.play()
            } else {
                exoPlayer?.pause()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            if (imageMode) {
                blurProgress = blurTarget()
                loadDailyImage()
            } else {
                createPlayer(holder)
            }
        }

        @OptIn(UnstableApi::class)
        private fun createPlayer(holder: SurfaceHolder) {
            val renderersFactory = DefaultRenderersFactory(applicationContext)
            val player = ExoPlayer.Builder(applicationContext, renderersFactory).build()
            exoPlayer = player

            player.apply {
                setVideoSurfaceHolder(holder)
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                loadSelectedVideo()
                val shouldPlay =
                    isPreview ||
                        !keyguardManager.isKeyguardLocked ||
                        repository.getLiveWallpaperPlaybackTrigger() == SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON
                playWhenReady = shouldPlay
            }
        }

        private fun releasePlayer() {
            exoPlayer?.release()
            exoPlayer = null
        }

        private fun applyMode() {
            val daily = repository.isLiveWallpaperDailyImageSelected()
            if (daily) {
                releasePlayer()
                imageMode = true
                blurProgress = blurTarget()
                if (surfaceReady) loadDailyImage()
            } else {
                imageMode = false
                imageLoadJob?.cancel()
                blurAnimator?.cancel()
                clearImageState()
                val player = exoPlayer
                if (player != null) {
                    loadSelectedVideo()
                } else if (surfaceReady) {
                    createPlayer(surfaceHolder)
                }
            }
        }

        @OptIn(UnstableApi::class)
        private fun loadSelectedVideo() {
            val videoName = repository.getLiveWallpaperSelectedVideo()
            val resId = resources.getIdentifier(videoName, "raw", packageName)
            val mediaItem =
                if (resId != 0) {
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

        private fun isLockScreenOnlyEngine(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                wallpaperFlags == WallpaperManager.FLAG_LOCK

        private fun blurAllowed(): Boolean = repository.isLiveWallpaperProgressiveBlurEnabled() && !isLockScreenOnlyEngine()

        private fun blurTarget(): Float =
            when {
                !blurAllowed() -> 0f
                isPreview -> 1f
                keyguardManager.isKeyguardLocked -> 0f
                else -> 1f
            }

        private fun setBlurImmediately(value: Float) {
            if (!imageMode) return
            blurAnimator?.cancel()
            blurProgress = value
            drawImage()
        }

        private fun animateBlurTo(target: Float) {
            if (!imageMode) return
            blurAnimator?.cancel()
            if (blurProgress == target) return
            blurAnimator =
                ValueAnimator.ofFloat(blurProgress, target).apply {
                    duration = 700
                    interpolator = DecelerateInterpolator(1.6f)
                    addUpdateListener {
                        blurProgress = it.animatedValue as Float
                        drawImage()
                    }
                    start()
                }
        }

        private fun loadDailyImage() {
            imageLoadJob?.cancel()
            imageLoadJob =
                scope.launch {
                    val bitmap = withContext(Dispatchers.IO) { resolveDailyBitmap() } ?: return@launch
                    if (!imageMode) return@launch
                    dailyBitmap = bitmap
                    blurCacheKey = null
                    drawImage()
                }
        }

        private suspend fun resolveDailyBitmap(): Bitmap? {
            val wallpaperRepository = WallpaperRepository()
            var id = repository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_LAST_ID).orEmpty()
            var url = repository.getString(SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL_MOBILE).orEmpty()
            if (url.isBlank()) {
                val info = wallpaperRepository.fetchTodayWallpaper() ?: return loadAnyCachedImage()
                id = info.id
                url = info.urlMobile
            }

            val imageDir = File(filesDir, CACHE_DIR).apply { mkdirs() }
            val key = id.ifBlank { url.hashCode().toString() }.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val cacheFile = File(imageDir, "$key.jpg")
            if (cacheFile.exists()) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { return it }
            }

            val downloaded = wallpaperRepository.downloadBitmap(url) ?: return loadAnyCachedImage()
            try {
                FileOutputStream(cacheFile).use { downloaded.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                imageDir.listFiles()?.filter { it != cacheFile }?.forEach { it.delete() }
            } catch (_: Exception) {
            }
            return downloaded
        }

        private fun loadAnyCachedImage(): Bitmap? =
            File(filesDir, CACHE_DIR)
                .listFiles()
                ?.maxByOrNull { it.lastModified() }
                ?.let { BitmapFactory.decodeFile(it.absolutePath) }

        private fun centerCropMatrix(
            bitmap: Bitmap,
            width: Int,
            height: Int,
        ): Matrix {
            val scale = maxOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
            val dx = (width - bitmap.width * scale) / 2f
            val dy = (height - bitmap.height * scale) / 2f
            return Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            }
        }

        private fun ensureBlurLayer(
            bitmap: Bitmap,
            matrix: Matrix,
            width: Int,
            height: Int,
        ) {
            val radius = resources.displayMetrics.density * BLUR_RADIUS_DP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // re-recorded every frame: the GPU side of the node gets dropped across screen off/lock
                val node = blurNode ?: RenderNode("progressiveBlur").also { blurNode = it }
                node.setPosition(0, 0, width, height)
                node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
                val recording = node.beginRecording(width, height)
                recording.drawBitmap(bitmap, matrix, bitmapPaint)
                node.endRecording()
                return
            }

            val key = "${System.identityHashCode(bitmap)}:$width:$height"
            if (key == blurCacheKey && blurFallback != null) return
            blurCacheKey = key
            blurFallback?.recycle()
            blurFallback = null
            val factor = 1f / (radius / 2f).coerceAtLeast(4f)
            val small =
                Bitmap.createBitmap(
                    (width * factor).toInt().coerceAtLeast(1),
                    (height * factor).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
            Canvas(small).drawBitmap(
                bitmap,
                Matrix(matrix).apply { postScale(factor, factor) },
                bitmapPaint,
            )
            blurFallback = small
        }

        private fun discardBlurNode() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) blurNode?.discardDisplayList()
            blurNode = null
        }

        private fun clearImageState() {
            dailyBitmap = null
            blurCacheKey = null
            discardBlurNode()
            blurFallback?.recycle()
            blurFallback = null
        }

        private fun drawImage() {
            if (!imageMode || !surfaceReady) return
            val bitmap = dailyBitmap ?: return
            val holder = surfaceHolder
            val canvas =
                try {
                    holder.lockHardwareCanvas()
                } catch (_: Exception) {
                    null
                } ?: return
            try {
                val width = canvas.width
                val height = canvas.height
                val matrix = centerCropMatrix(bitmap, width, height)
                canvas.drawColor(Color.BLACK)
                canvas.drawBitmap(bitmap, matrix, bitmapPaint)
                if (blurProgress > 0f && repository.isLiveWallpaperProgressiveBlurEnabled()) {
                    ensureBlurLayer(bitmap, matrix, width, height)
                    val edge = height * repository.getLiveWallpaperProgressiveBlurHeight().coerceIn(0.02f, 0.5f)
                    drawBlurredEdge(canvas, width, height, edge, top = true)
                    drawBlurredEdge(canvas, width, height, edge, top = false)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawBlurredEdge(
            canvas: Canvas,
            width: Int,
            height: Int,
            edge: Float,
            top: Boolean,
        ) {
            val start = if (top) 0f else height.toFloat()
            val end = if (top) edge else height - edge
            val alpha = (blurProgress * 255).toInt().coerceIn(0, 255)
            val stops = floatArrayOf(0f, 0.45f, 1f)

            val save =
                if (top) {
                    canvas.saveLayer(0f, 0f, width.toFloat(), edge, null)
                } else {
                    canvas.saveLayer(0f, height - edge, width.toFloat(), height.toFloat(), null)
                }
            val node = blurNode
            val fallback = blurFallback
            if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                canvas.drawRenderNode(node)
            } else if (fallback != null) {
                canvas.drawBitmap(fallback, null, Rect(0, 0, width, height), bitmapPaint)
            }
            maskPaint.shader =
                LinearGradient(
                    0f,
                    start,
                    0f,
                    end,
                    intArrayOf(Color.argb(alpha, 0, 0, 0), Color.argb((alpha * 0.75f).toInt(), 0, 0, 0), Color.TRANSPARENT),
                    stops,
                    Shader.TileMode.CLAMP,
                )
            if (top) {
                canvas.drawRect(0f, 0f, width.toFloat(), edge, maskPaint)
            } else {
                canvas.drawRect(0f, height - edge, width.toFloat(), height.toFloat(), maskPaint)
            }
            canvas.restoreToCount(save)

            val scrimAlpha = (blurProgress * SCRIM_ALPHA * 255).toInt()
            scrimPaint.shader =
                LinearGradient(
                    0f,
                    start,
                    0f,
                    end,
                    Color.argb(scrimAlpha, 0, 0, 0),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
            if (top) {
                canvas.drawRect(0f, 0f, width.toFloat(), edge, scrimPaint)
            } else {
                canvas.drawRect(0f, height - edge, width.toFloat(), height.toFloat(), scrimPaint)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            f: Int,
            w: Int,
            h: Int,
        ) {
            super.onSurfaceChanged(holder, f, w, h)
            exoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            drawImage()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            drawImage()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            blurAnimator?.cancel()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            super.onDestroy()
            executor.removeCallbacksAndMessages(null)
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
            }
            repository.unregisterOnSharedPreferenceChangeListener(prefsListener)
            repository.unregisterLiveWallpaperPrefsListener(livePrefsListener)
            blurAnimator?.cancel()
            scope.cancel()
            clearImageState()
            releasePlayer()
        }
    }

    companion object {
        private const val CACHE_DIR = "live_daily_wallpaper"
        private const val BLUR_RADIUS_DP = 28f
        private const val SCRIM_ALPHA = 0.18f
    }
}
