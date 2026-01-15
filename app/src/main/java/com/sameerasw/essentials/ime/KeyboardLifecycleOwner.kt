package com.sameerasw.essentials.ime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * A specialized LifecycleOwner for the Keyboard IME.
 * Allows granular control over lifecycle states (Created, Resumed, Destroyed)
 * to match the InputMethodService lifecycle events.
 */
class KeyboardLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    /** Call once when the IME view is created */
    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /** Call when the view becomes visible / active */
    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Call when the view is hidden (but not destroyed) */
    fun onPause() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /** Call when the IME is permanently destroyed */
    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
