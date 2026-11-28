/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: LiveWallpaperService.kt
 * Description: Background service component for LiveWallpaperService.kt.
 */

package com.sameerasw.essentials.services

import android.R as AndroidR
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.animation.DecelerateInterpolator
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.palette.graphics.Palette
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.data.repository.WallpaperRepository
import com.sameerasw.essentials.services.handlers.WallpaperDockBridge
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
        private var revealProgress = 0f
        private var revealAnimator: ValueAnimator? = null
        private var zoomProgress = 0f
        private var dockRect: RectF? = null
        private var dockAlpha = 0f
        private var dockAnimator: ValueAnimator? = null
        private var zoomAnimator: ValueAnimator? = null
        private var mediaTracker: LiveWallpaperMediaTracker? = null
        private val artHandler = Handler(Looper.getMainLooper())
        private var currentArt: Bitmap? = null
        private var currentArtKey: String? = null
        private var outgoingArt: Bitmap? = null
        private var currentArtShade = Color.BLACK
        private var outgoingArtShade = Color.BLACK
        private val artFadePaint = Paint()
        private val edgeNodes = arrayOfNulls<RenderNode>(EDGE_BLUR_STEPS * 2)
        private var isMusicPlaying = false
        private var artProgress = 0f
        private var artSwap = 1f
        private var artAnimator: ValueAnimator? = null
        private var artSwapAnimator: ValueAnimator? = null
        private val artPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private var hideArtPending = false
        private val hideArtRunnable =
            Runnable {
                hideArtPending = false
                isMusicPlaying = false
                animateArtTo(0f)
            }
        private var drawScheduled = false
        private val frameCallback =
            Choreographer.FrameCallback {
                drawScheduled = false
                drawImage()
            }

        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val maskPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        private val scrimPaint = Paint()
        private val dockClipPath = Path()
        private var dockBlurNode: RenderNode? = null
        private val dockTintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val dockStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        private val livePrefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    SettingsRepository.KEY_LIVE_WALLPAPER_SELECTED_VIDEO -> applyMode()
                    SettingsRepository.KEY_LIVE_WALLPAPER_ALBUM_ART -> {
                        updateMediaTracking()
                        animateArtTo(artTarget())
                    }
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
                    SettingsRepository.KEY_LIVE_WALLPAPER_DOCK,
                    SettingsRepository.KEY_LIVE_WALLPAPER_DOCK_PADDING,
                    SettingsRepository.KEY_LIVE_WALLPAPER_DOCK_OPACITY,
                    -> {
                        revealProgress = revealTarget()
                        requestDraw()
                    }
                }
            }

        private val prefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == SettingsRepository.KEY_DAILY_WALLPAPER_LAST_URL_MOBILE && imageMode) {
                    loadDailyImage()
                }
                if (key == SettingsRepository.KEY_AOD_WALLPAPER_MEDIA_EXCLUDED_APPS) mediaTracker?.refresh()
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
                            animateRevealTo(if (revealAllowed()) 1f else 0f)
                            animateZoomTo(0f, ZOOM_OUT_DURATION)
                            animateArtTo(artTarget(locked = false))
                        }

                        Intent.ACTION_SCREEN_OFF -> {
                            setRevealImmediately(0f)
                            setZoomImmediately(0f)
                            setArtImmediately(artTarget(locked = true))
                            discardBlurNode()
                            executor.removeCallbacksAndMessages(null)
                            executor.postDelayed({
                                exoPlayer?.pause()
                                exoPlayer?.seekTo(0)
                            }, 500)
                        }

                        Intent.ACTION_CONFIGURATION_CHANGED -> requestDraw()

                        Intent.ACTION_SCREEN_ON -> {
                            setArtImmediately(artTarget())
                            if (keyguardManager.isKeyguardLocked) {
                                setRevealImmediately(0f)
                                if (zoomAllowed()) animateZoomTo(1f, ZOOM_IN_DURATION)
                            } else {
                                animateRevealTo(revealTarget())
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
                    addAction(Intent.ACTION_CONFIGURATION_CHANGED)
                }
            registerReceiver(receiver, filter)

            repository.registerOnSharedPreferenceChangeListener(prefsListener)
            repository.registerLiveWallpaperPrefsListener(livePrefsListener)
            if (imageMode) onDailyLiveActivated()
            updateMediaTracking()
            scope.launch { WallpaperDockBridge.bounds.collect { onDockBoundsChanged(it) } }
        }

        private fun updateMediaTracking() {
            val active = imageMode && !isPreview && repository.getLiveWallpaperAlbumArtMode() != SettingsRepository.LIVE_WALLPAPER_ALBUM_ART_NONE
            if (active) {
                val tracker =
                    mediaTracker ?: LiveWallpaperMediaTracker(applicationContext, repository, ::onMediaChanged).also { mediaTracker = it }
                tracker.start()
            } else {
                mediaTracker?.stop()
                artHandler.removeCallbacks(hideArtRunnable)
                hideArtPending = false
                isMusicPlaying = false
                animateArtTo(0f)
            }
        }

        private fun onMediaChanged(
            art: Bitmap?,
            key: String?,
            playing: Boolean,
        ) {
            if (!playing || art == null) {
                // short grace period so track skips and brief pauses don't flash the wallpaper
                if (isMusicPlaying && !hideArtPending) {
                    hideArtPending = true
                    artHandler.postDelayed(hideArtRunnable, ART_HIDE_DELAY)
                }
                return
            }
            artHandler.removeCallbacks(hideArtRunnable)
            hideArtPending = false
            isMusicPlaying = true
            if (key != currentArtKey) {
                val previous = currentArt
                val previousShade = currentArtShade
                currentArtKey = key
                currentArt = art
                currentArtShade = darkAccentShade(art)
                if (previous != null && artProgress > 0f) crossfadeArt(previous, previousShade)
            } else {
                currentArt = art
            }
            animateArtTo(artTarget())
            requestDraw()
        }

        private fun artTarget(locked: Boolean = keyguardManager.isKeyguardLocked): Float {
            if (!imageMode || currentArt == null || !isMusicPlaying) return 0f
            return when (repository.getLiveWallpaperAlbumArtMode()) {
                SettingsRepository.LIVE_WALLPAPER_ALBUM_ART_LOCK -> if (locked) 1f else 0f
                SettingsRepository.LIVE_WALLPAPER_ALBUM_ART_HOME -> if (locked) 0f else 1f
                SettingsRepository.LIVE_WALLPAPER_ALBUM_ART_BOTH -> 1f
                else -> 0f
            }
        }

        private fun setArtImmediately(value: Float) {
            if (!imageMode) return
            artAnimator?.cancel()
            artProgress = value
            if (value == 0f) clearArtIfHidden()
            requestDraw()
        }

        private fun animateArtTo(target: Float) {
            artAnimator?.cancel()
            if (artProgress == target) {
                if (target == 0f) clearArtIfHidden()
                return
            }
            artAnimator =
                ValueAnimator.ofFloat(artProgress, target).apply {
                    duration = ART_FADE_DURATION
                    interpolator = DecelerateInterpolator(1.4f)
                    addUpdateListener {
                        artProgress = it.animatedValue as Float
                        if (artProgress == 0f) clearArtIfHidden()
                        requestDraw()
                    }
                    start()
                }
        }

        private fun crossfadeArt(
            previous: Bitmap,
            previousShade: Int,
        ) {
            artSwapAnimator?.cancel()
            outgoingArt = previous
            outgoingArtShade = previousShade
            artSwap = 0f
            artSwapAnimator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = ART_FADE_DURATION
                    interpolator = DecelerateInterpolator(1.4f)
                    addUpdateListener {
                        artSwap = it.animatedValue as Float
                        if (artSwap >= 1f) outgoingArt = null
                        requestDraw()
                    }
                    start()
                }
        }

        private fun clearArtIfHidden() {
            if (isMusicPlaying) return
            currentArt = null
            currentArtKey = null
            outgoingArt = null
        }

        private fun onDockBoundsChanged(bounds: Rect?) {
            val target = bounds?.let { RectF(it) }
            val from = dockRect
            val fromAlpha = dockAlpha
            val targetAlpha = if (target != null) 1f else 0f
            dockAnimator?.cancel()
            if (target != null && from == null) dockRect = RectF(target)
            dockAnimator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = DOCK_ANIM_DURATION
                    interpolator = DecelerateInterpolator(1.5f)
                    addUpdateListener {
                        val t = it.animatedValue as Float
                        dockAlpha = fromAlpha + (targetAlpha - fromAlpha) * t
                        if (target != null && from != null) {
                            dockRect =
                                RectF(
                                    from.left + (target.left - from.left) * t,
                                    from.top + (target.top - from.top) * t,
                                    from.right + (target.right - from.right) * t,
                                    from.bottom + (target.bottom - from.bottom) * t,
                                )
                        }
                        if (t >= 1f && target == null) dockRect = null
                        requestDraw()
                    }
                    start()
                }
        }

        // the live wallpaper takes over daily updates, so the static auto-apply is switched off
        private fun onDailyLiveActivated() {
            if (isPreview) return
            if (repository.getBoolean(SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE, false)) {
                repository.putBoolean(SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE, false)
                repository.remove(SettingsRepository.KEY_DAILY_WALLPAPER_AUTO_UPDATE_TIME)
                repository.remove(SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT)
            }
            DailyWallpaperWorker.ensurePeriodic(applicationContext)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (imageMode) {
                if (visible) {
                    revealAnimator?.cancel()
                    revealProgress = revealTarget()
                    if (!keyguardManager.isKeyguardLocked) {
                        zoomAnimator?.cancel()
                        zoomProgress = 0f
                    }
                    artAnimator?.cancel()
                    artProgress = artTarget()
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
                revealProgress = revealTarget()
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
                onDailyLiveActivated()
                revealProgress = revealTarget()
                updateMediaTracking()
                if (surfaceReady) loadDailyImage()
            } else {
                imageMode = false
                imageLoadJob?.cancel()
                revealAnimator?.cancel()
                zoomAnimator?.cancel()
                zoomProgress = 0f
                updateMediaTracking()
                artAnimator?.cancel()
                artProgress = 0f
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

        // drives everything that only shows once unlocked: edge blur and the dock container
        private fun revealAllowed(): Boolean =
            (repository.isLiveWallpaperProgressiveBlurEnabled() || repository.isLiveWallpaperDockEnabled()) &&
                !isLockScreenOnlyEngine()

        private fun revealTarget(): Float =
            when {
                !revealAllowed() -> 0f
                isPreview -> 1f
                keyguardManager.isKeyguardLocked -> 0f
                else -> 1f
            }

        private fun setRevealImmediately(value: Float) {
            if (!imageMode) return
            revealAnimator?.cancel()
            revealProgress = value
            drawImage()
        }

        private fun zoomAllowed(): Boolean = !isPreview && repository.isLiveWallpaperZoomEffectEnabled()

        private fun setZoomImmediately(value: Float) {
            if (!imageMode) return
            zoomAnimator?.cancel()
            zoomProgress = value
            requestDraw()
        }

        private fun animateZoomTo(
            target: Float,
            durationMs: Long,
        ) {
            if (!imageMode) return
            zoomAnimator?.cancel()
            if (zoomProgress == target) return
            zoomAnimator =
                ValueAnimator.ofFloat(zoomProgress, target).apply {
                    duration = durationMs
                    interpolator = DecelerateInterpolator(1.4f)
                    addUpdateListener {
                        zoomProgress = it.animatedValue as Float
                        requestDraw()
                    }
                    start()
                }
        }

        private fun requestDraw() {
            if (drawScheduled) return
            drawScheduled = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }

        private fun animateRevealTo(target: Float) {
            if (!imageMode) return
            revealAnimator?.cancel()
            if (revealProgress == target) return
            revealAnimator =
                ValueAnimator.ofFloat(revealProgress, target).apply {
                    duration = 700
                    interpolator = DecelerateInterpolator(1.6f)
                    addUpdateListener {
                        revealProgress = it.animatedValue as Float
                        requestDraw()
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

        private fun sceneMatrix(
            bitmap: Bitmap,
            width: Int,
            height: Int,
        ): Matrix {
            val zoom = 1f + ZOOM_AMOUNT * zoomProgress
            return centerCropMatrix(bitmap, width, height).apply { postScale(zoom, zoom, width / 2f, height / 2f) }
        }

        // daily image with the album art layered over it; shared by the frame and both blur layers
        private fun drawScene(
            canvas: Canvas,
            width: Int,
            height: Int,
        ) {
            val base = dailyBitmap ?: return
            canvas.drawBitmap(base, sceneMatrix(base, width, height), bitmapPaint)
            if (artProgress <= 0f) return
            val outgoing = outgoingArt
            if (outgoing != null) drawArt(canvas, outgoing, outgoingArtShade, artProgress, width, height)
            currentArt?.let {
                drawArt(canvas, it, currentArtShade, artProgress * if (outgoing != null) artSwap else 1f, width, height)
            }
        }

        // same look as the AOD album art: cropped art with accent tinted fades from the top and bottom
        private fun drawArt(
            canvas: Canvas,
            art: Bitmap,
            shade: Int,
            alpha: Float,
            width: Int,
            height: Int,
        ) {
            if (art.isRecycled || alpha <= 0f) return
            val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
            artPaint.alpha = alphaInt
            canvas.drawBitmap(art, sceneMatrix(art, width, height), artPaint)

            val fade = height * ART_FADE_FRACTION
            val tinted = withAlpha(shade, alphaInt.toFloat())
            artFadePaint.shader = LinearGradient(0f, 0f, 0f, fade, tinted, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), fade, artFadePaint)
            artFadePaint.shader =
                LinearGradient(0f, height - fade, 0f, height.toFloat(), Color.TRANSPARENT, tinted, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, height - fade, width.toFloat(), height.toFloat(), artFadePaint)
        }

        private fun darkAccentShade(bitmap: Bitmap): Int {
            val palette =
                try {
                    Palette.from(bitmap).maximumColorCount(24).generate()
                } catch (_: Exception) {
                    null
                }
            val accent =
                palette?.vibrantSwatch?.rgb
                    ?: palette?.lightVibrantSwatch?.rgb
                    ?: palette?.dominantSwatch?.rgb
                    ?: palette?.mutedSwatch?.rgb
                    ?: Color.BLACK
            val hsv = FloatArray(3)
            Color.colorToHSV(accent, hsv)
            hsv[1] = hsv[1].coerceIn(0.6f, 1f)
            hsv[2] = (hsv[2] * 0.65f).coerceIn(0.4f, 0.65f)
            return Color.HSVToColor(hsv)
        }

        // stacked blur steps whose radius grows toward the screen edge; each step masks itself inside its node
        private fun drawProgressiveEdge(
            canvas: Canvas,
            width: Int,
            height: Int,
            edge: Float,
            top: Boolean,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
            val edgePx = edge.toInt().coerceIn(1, height)
            val offsetY = if (top) 0f else (height - edgePx).toFloat()
            val maxRadius = resources.displayMetrics.density * BLUR_RADIUS_DP * revealProgress
            if (maxRadius >= 0.5f) {
                for (step in 0 until EDGE_BLUR_STEPS) {
                    val radius = maxRadius * (step + 1) / EDGE_BLUR_STEPS
                    if (radius < 0.5f) continue
                    val index = step + if (top) 0 else EDGE_BLUR_STEPS
                    val node = edgeNodes[index] ?: RenderNode("edgeBlur$index").also { edgeNodes[index] = it }

                    // distance from the screen edge where this step is fully on, and where it has faded out
                    val fullUntil = edgePx * (1f - (step + 1f) / EDGE_BLUR_STEPS)
                    val goneAt = edgePx * (1f - step.toFloat() / EDGE_BLUR_STEPS)
                    val mask =
                        if (top) {
                            LinearGradient(0f, fullUntil, 0f, goneAt, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                        } else {
                            LinearGradient(0f, edgePx - fullUntil, 0f, edgePx - goneAt, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                        }
                    node.setPosition(0, 0, width, edgePx)
                    node.setRenderEffect(
                        RenderEffect.createBlendModeEffect(
                            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
                            RenderEffect.createShaderEffect(mask),
                            BlendMode.DST_IN,
                        ),
                    )
                    val recording = node.beginRecording(width, edgePx)
                    recording.translate(0f, -offsetY)
                    drawScene(recording, width, height)
                    node.endRecording()

                    val save = canvas.save()
                    canvas.translate(0f, offsetY)
                    canvas.drawRenderNode(node)
                    canvas.restoreToCount(save)
                }
            }

            val start = if (top) 0f else height.toFloat()
            val end = if (top) edge else height - edge
            scrimPaint.shader =
                LinearGradient(
                    0f,
                    start,
                    0f,
                    end,
                    Color.argb((revealProgress * SCRIM_ALPHA * 255).toInt(), 0, 0, 0),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
            canvas.drawRect(0f, if (top) 0f else height - edge, width.toFloat(), if (top) edge else height.toFloat(), scrimPaint)
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
                drawScene(recording, width, height)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blurNode?.discardDisplayList()
                dockBlurNode?.discardDisplayList()
                edgeNodes.forEach { it?.discardDisplayList() }
            }
            blurNode = null
            dockBlurNode = null
            edgeNodes.fill(null)
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
                val matrix = sceneMatrix(bitmap, width, height)
                canvas.drawColor(Color.BLACK)
                drawScene(canvas, width, height)
                val showEdges = revealProgress > 0f && repository.isLiveWallpaperProgressiveBlurEnabled()
                val dock = dockRect
                val showDock =
                    revealProgress > 0f && dockAlpha > 0f && dock != null && !isPreview &&
                        repository.isLiveWallpaperDockEnabled()
                val legacyBlur = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                if (legacyBlur && (showEdges || showDock)) ensureBlurLayer(bitmap, matrix, width, height)
                if (showEdges) {
                    val edge = height * repository.getLiveWallpaperProgressiveBlurHeight().coerceIn(0.02f, 0.5f)
                    if (legacyBlur) {
                        drawBlurredEdge(canvas, width, height, edge, top = true)
                        drawBlurredEdge(canvas, width, height, edge, top = false)
                    } else {
                        drawProgressiveEdge(canvas, width, height, edge, top = true)
                        drawProgressiveEdge(canvas, width, height, edge, top = false)
                    }
                }
                if (showDock && dock != null) drawDock(canvas, dock, width, height)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawDock(
            canvas: Canvas,
            icons: RectF,
            width: Int,
            height: Int,
        ) {
            val density = resources.displayMetrics.density
            val padding = repository.getLiveWallpaperDockPadding() * density
            val container = RectF(icons.left - padding, icons.top - padding, icons.right + padding, icons.bottom + padding)
            val corner = minOf(container.height() / 2f, DOCK_CORNER_DP * density)
            val progress = (revealProgress * dockAlpha).coerceIn(0f, 1f)
            val alpha = (progress * 255).toInt()

            dockClipPath.reset()
            dockClipPath.addRoundRect(container, corner, corner, Path.Direction.CW)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val radius = BLUR_RADIUS_DP * density * progress
                if (radius >= 0.5f) {
                    val node = dockBlurNode ?: RenderNode("dockBlur").also { dockBlurNode = it }
                    node.setPosition(0, 0, width, height)
                    node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
                    val recording = node.beginRecording(width, height)
                    drawScene(recording, width, height)
                    node.endRecording()
                    val save = canvas.save()
                    canvas.clipPath(dockClipPath)
                    canvas.drawRenderNode(node)
                    canvas.restoreToCount(save)
                }
            } else {
                blurFallback?.let {
                    val save = canvas.save()
                    canvas.clipPath(dockClipPath)
                    bitmapPaint.alpha = alpha
                    canvas.drawBitmap(it, null, zoomedRect(width, height), bitmapPaint)
                    bitmapPaint.alpha = 255
                    canvas.restoreToCount(save)
                }
            }

            val night = isNightMode()
            val opacity = repository.getLiveWallpaperDockOpacity().coerceIn(0f, 1f)
            dockTintPaint.color = withAlpha(dockTintColor(night), alpha * opacity)
            canvas.drawPath(dockClipPath, dockTintPaint)
            dockStrokePaint.strokeWidth = density
            dockStrokePaint.color = withAlpha(Color.WHITE, alpha * opacity * if (night) DOCK_STROKE_RATIO_DARK else DOCK_STROKE_RATIO_LIGHT)
            canvas.drawPath(dockClipPath, dockStrokePaint)
        }

        private fun isNightMode(): Boolean =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // material you accent tones where available, plain black/white before Android 12
        private fun dockTintColor(night: Boolean): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getColor(if (night) AndroidR.color.system_accent1_900 else AndroidR.color.system_accent1_50)
            } else if (night) {
                Color.BLACK
            } else {
                Color.WHITE
            }

        private fun withAlpha(
            color: Int,
            alpha: Float,
        ): Int = Color.argb(alpha.toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

        private fun zoomedRect(
            width: Int,
            height: Int,
        ): RectF {
            val zoom = 1f + ZOOM_AMOUNT * zoomProgress
            val dx = width * (zoom - 1f) / 2f
            val dy = height * (zoom - 1f) / 2f
            return RectF(-dx, -dy, width + dx, height + dy)
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
            val alpha = (revealProgress * 255).toInt().coerceIn(0, 255)
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
                canvas.drawBitmap(fallback, null, zoomedRect(width, height), bitmapPaint)
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

            val scrimAlpha = (revealProgress * SCRIM_ALPHA * 255).toInt()
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
            revealAnimator?.cancel()
            zoomAnimator?.cancel()
            dockAnimator?.cancel()
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            drawScheduled = false
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
            mediaTracker?.stop()
            artHandler.removeCallbacksAndMessages(null)
            artAnimator?.cancel()
            artSwapAnimator?.cancel()
            revealAnimator?.cancel()
            zoomAnimator?.cancel()
            dockAnimator?.cancel()
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            scope.cancel()
            clearImageState()
            releasePlayer()
        }
    }

    companion object {
        private const val CACHE_DIR = "live_daily_wallpaper"

        fun latestDailyImageFile(context: Context): File? =
            File(context.filesDir, CACHE_DIR).listFiles()?.maxByOrNull { it.lastModified() }

        private const val BLUR_RADIUS_DP = 28f
        private const val ZOOM_AMOUNT = 0.06f
        private const val DOCK_CORNER_DP = 32f
        private const val DOCK_STROKE_RATIO_LIGHT = 0.7f
        private const val DOCK_STROKE_RATIO_DARK = 0.25f
        private const val DOCK_ANIM_DURATION = 320L
        private const val ZOOM_IN_DURATION = 1600L
        private const val ZOOM_OUT_DURATION = 650L
        private const val SCRIM_ALPHA = 0.18f
        private const val ART_FADE_DURATION = 650L
        private const val ART_FADE_FRACTION = 0.4f
        private const val EDGE_BLUR_STEPS = 4
        private const val ART_HIDE_DELAY = 2500L
    }
}
