package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.ui.components.sheets.EssentialHubContent
import com.sameerasw.essentials.ui.theme.EssentialsTheme

class EssentialHubHandler(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var isHubVisible = false
    private val isVisibleState = mutableStateOf(false)

    fun toggleHub() {
        if (isHubVisible) {
            hideHub()
        } else {
            showHub()
        }
    }

    fun showHub() {
        if (isHubVisible) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            dimAmount = 0.01f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = 50
            }
        }

        val lifecycleOwner = HubLifecycleOwner()
        lifecycleOwner.onCreate()

        overlayView = ComposeView(service).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            
            setContent {
                val visible by isVisibleState
                EssentialsTheme {
                    EssentialHubContent(
                        isVisible = visible,
                        onDismiss = { hideHub() },
                        onProgressChanged = { progress ->
                            updateBackgroundEffects(progress)
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(overlayView, params)
            isHubVisible = true
            
            val animator = android.animation.ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 300
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    updateBackgroundEffects(progress)
                }
            }
            animator.start()

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isVisibleState.value = true
            }, 50)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBackgroundEffects(progress: Float) {
        val invertedProgress = (1f - progress).coerceIn(0f, 1f)
        val dimAmount = (invertedProgress * 0.5f).coerceAtLeast(0.01f) // Keep tiny dim for "behind" pass
        val blurRadius = (invertedProgress * 50).toInt()

        val updatedParams = overlayView?.layoutParams as? WindowManager.LayoutParams
        if (updatedParams != null && overlayView != null) {
            updatedParams.dimAmount = dimAmount
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                updatedParams.blurBehindRadius = blurRadius
//                updatedParams.backgroundBlurRadius = blurRadius
            }
            try {
                windowManager.updateViewLayout(overlayView, updatedParams)
            } catch (_: Exception) {}
        }
    }

    fun hideHub() {
        if (!isHubVisible || overlayView == null) return
        isVisibleState.value = false
        
        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                updateBackgroundEffects(progress)
            }
        }
        animator.start()

        // Wait for exit animation (300ms defined in EssentialHubContent)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            removeViewImmediately()
        }, 300)
    }

    private fun removeViewImmediately() {
        try {
            if (isHubVisible && overlayView != null) {
                windowManager.removeView(overlayView)
                overlayView = null
                isHubVisible = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class HubLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
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
}
