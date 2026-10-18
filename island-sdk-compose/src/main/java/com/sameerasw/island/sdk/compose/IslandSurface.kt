package com.sameerasw.island.sdk.compose

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.MotionEvent
import android.view.SurfaceControlViewHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.island.sdk.SurfaceRequest

class IslandSurface(
    context: Context,
    request: SurfaceRequest,
    content: @Composable () -> Unit,
) {
    private val owner = SurfaceOwner()
    private val host: SurfaceControlViewHost
    private val view: ComposeView

    val surfacePackage: SurfaceControlViewHost.SurfacePackage?

    init {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(request.displayId)
            ?: context.getSystemService(DisplayManager::class.java).getDisplay(0)
        val displayContext = context.createDisplayContext(display)
        host = SurfaceControlViewHost(displayContext, display, request.hostToken)
        view = ComposeView(displayContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent(content)
        }
        host.setView(view, request.widthPx, request.heightPx)
        owner.start()
        surfacePackage = host.surfacePackage
    }

    fun resize(widthPx: Int, heightPx: Int) {
        host.relayout(widthPx, heightPx)
    }

    fun dispatchTouch(event: MotionEvent) {
        view.dispatchTouchEvent(event)
    }

    fun release() {
        owner.stop()
        view.disposeComposition()
        host.release()
    }

    private class SurfaceOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(null)
        }

        fun start() {
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
