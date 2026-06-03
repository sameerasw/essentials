package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.OverlayHelper

class PocketModeHandler(private val service: AccessibilityService) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    var isBypassed = false
    var isOverlayVisible = false

    private val handler = Handler(Looper.getMainLooper())
    private var isPending = false
    private val showOverlayRunnable = Runnable {
        isPending = false
        if (!isBypassed) {
            showOverlay()
        }
    }
    private val screenOffRunnable = Runnable {
        if (isOverlayVisible) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }
        }
    }

    fun showOverlay() {
        if (isOverlayVisible || overlayView != null) return

        Log.d("PocketModeHandler", "Showing pocket mode overlay")
        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val context = service

        val owner = OverlayLifecycleOwner()
        lifecycleOwner = owner

        val frameLayout = FrameLayout(context)
        owner.onCreate()
        frameLayout.setViewTreeLifecycleOwner(owner)
        frameLayout.setViewTreeSavedStateRegistryOwner(owner)
        frameLayout.setViewTreeViewModelStoreOwner(owner)

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                PocketModeOverlayContent()
            }
        }
        frameLayout.addView(composeView)

        overlayView = frameLayout

        val params = OverlayHelper.createOverlayLayoutParams(
            overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags = 0,
            isTouchable = true
        )

        try {
            windowManager?.addView(frameLayout, params)
            isOverlayVisible = true
            handler.postDelayed(screenOffRunnable, 5000L)
        } catch (e: Exception) {
            Log.e("PocketModeHandler", "Error adding pocket overlay", e)
            removeOverlay()
        }
    }

    fun removeOverlay() {
        handler.removeCallbacks(screenOffRunnable)
        if (overlayView != null) {
            Log.d("PocketModeHandler", "Removing pocket mode overlay")
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e("PocketModeHandler", "Error removing pocket overlay view", e)
            }
            overlayView = null
        }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        isOverlayVisible = false
        isBypassed = false
    }

    fun onProximityChanged(isBlocked: Boolean, isLightDark: Boolean, useLightSensor: Boolean, triggerDelayMs: Long = 3000L) {
        val shouldShow = isBlocked && (!useLightSensor || isLightDark)
        if (shouldShow) {
            if (!isBypassed && !isOverlayVisible && !isPending) {
                isPending = true
                handler.postDelayed(showOverlayRunnable, triggerDelayMs)
            }
        } else {
            if (isPending) {
                handler.removeCallbacks(showOverlayRunnable)
                isPending = false
            }
            removeOverlay()
        }
    }

    fun onScreenOff() {
        handler.removeCallbacks(showOverlayRunnable)
        handler.removeCallbacks(screenOffRunnable)
        isPending = false
        removeOverlay()
        isBypassed = false
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore = store

        fun onCreate() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    @Composable
    private fun PocketModeOverlayContent() {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Pulsing circle at top center (near front camera/proximity sensor)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 140.dp)
                    .size(52.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    )
                    .background(Color.White.copy(alpha = 0.3f), shape = CircleShape)
            )

            // Center details
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.pocket_mode_active),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = context.getString(R.string.pocket_mode_dismiss_hint),
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
