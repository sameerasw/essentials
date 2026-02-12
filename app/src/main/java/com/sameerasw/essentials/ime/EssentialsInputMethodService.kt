package com.sameerasw.essentials.ime

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.ime.KeyboardInputView
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EssentialsInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, ClipboardManager.OnPrimaryClipChangedListener {
    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store by lazy { ViewModelStore() }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }
    private val suggestionEngine by lazy { SuggestionEngine(this) }
    
    private lateinit var clipboardManager: ClipboardManager
    private val _clipboardHistory = MutableStateFlow<List<String>>(emptyList())
    val clipboardHistory: StateFlow<List<String>> = _clipboardHistory.asStateFlow()
    
    private var currentKeyboardShape: Int = 0
    private var currentKeyboardRoundness: Float = 24f
    private var composedInputView: View? = null
    
    // Undo Stack
    private val undoStack = java.util.ArrayDeque<String>()

    // Suggestion Lookup Job
    private var lookupJob: Job? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        // Initialize suggestion engine
        suggestionEngine.initialize(lifecycleScope)
        
        try {
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            if (prefs.getBoolean(SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED, true)) {
                clipboardManager.addPrimaryClipChangedListener(this)
                updateClipboardHistory()
                Log.d("EssentialsIME", "Clipboard listener registered")
            }
        } catch (e: Exception) {
            Log.e("EssentialsIME", "Error init clipboard", e)
        }
    }
    
    override fun onPrimaryClipChanged() {
        Log.d("EssentialsIME", "Clipboard changed")
        updateClipboardHistory()
    }

    private fun updateClipboardHistory() {
        if (!::clipboardManager.isInitialized) return
        if (!clipboardManager.hasPrimaryClip()) return
        
        try {
            val clip = clipboardManager.primaryClip ?: return
            if (clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                Log.d("EssentialsIME", "Clipboard item: $text")
                if (!text.isNullOrBlank()) {
                    val current = _clipboardHistory.value.toMutableList()
                    // Remove if exists to move to top
                    current.remove(text)
                    current.add(0, text)
                    // Keep last 5
                    if (current.size > 5) {
                        _clipboardHistory.value = current.take(5)
                    } else {
                        _clipboardHistory.value = current
                    }
                }
            }
        } catch (e: Exception) {
             Log.e("EssentialsIME", "Error reading clipboard", e)
        }
    }

    override fun onCreateInputView(): View {
        // Move to STARTED state before creating the view to ensure
        // the lifecycle is ready when the ComposeView attaches
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val view = ComposeView(this)

        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)

        view.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        view.setContent {
            EssentialsTheme {
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { context.getSharedPreferences("essentials_prefs", MODE_PRIVATE) }
                
                // State variables for settings
                var rawKeyboardHeight by remember { mutableFloatStateOf(prefs.getFloat(SettingsRepository.KEY_KEYBOARD_HEIGHT, 400f)) }
                // Migration check
                if (rawKeyboardHeight < 100f) {
                     rawKeyboardHeight = 400f
                     prefs.edit().putFloat(SettingsRepository.KEY_KEYBOARD_HEIGHT, 280f).apply()
                }
                var keyboardHeight by remember { mutableFloatStateOf(rawKeyboardHeight) }
                var bottomPadding by remember { mutableFloatStateOf(prefs.getFloat(SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING, 0f)) }
                var keyboardRoundness by remember { mutableFloatStateOf(prefs.getFloat(SettingsRepository.KEY_KEYBOARD_ROUNDNESS, 24f)) }
                var keyboardShape by remember { mutableIntStateOf(prefs.getInt(SettingsRepository.KEY_KEYBOARD_SHAPE, 0)) }
                
                // Sync with internal state
                LaunchedEffect(keyboardRoundness, keyboardShape) {
                    currentKeyboardRoundness = keyboardRoundness
                    currentKeyboardShape = keyboardShape
                    // Trigger insets re-computation when these change
                    if (window?.window?.decorView?.isAttachedToWindow == true) {
                         window?.window?.decorView?.requestLayout()
                    }
                }
                
                var isFunctionsBottom by remember { mutableStateOf(prefs.getBoolean(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM, false)) }
                var functionsPadding by remember { mutableFloatStateOf(prefs.getFloat(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING, 0f)) }
                var isHapticsEnabled by remember { mutableStateOf(prefs.getBoolean(SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED, true)) }
                var hapticStrength by remember { mutableFloatStateOf(prefs.getFloat(SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH, 0.5f)) }
                var isAlwaysDark by remember { mutableStateOf(prefs.getBoolean(SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK, false)) }
                var isPitchBlack by remember { mutableStateOf(prefs.getBoolean(SettingsRepository.KEY_KEYBOARD_PITCH_BLACK, false)) }

                var isKeyboardClipboardEnabled by remember { mutableStateOf(prefs.getBoolean(SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED, true)) }

                // Observe SharedPreferences changes
                DisposableEffect(prefs) {
                    val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        when (key) {
                            SettingsRepository.KEY_KEYBOARD_HEIGHT -> {
                                val height = sharedPreferences.getFloat(SettingsRepository.KEY_KEYBOARD_HEIGHT, 280f)
                                keyboardHeight = if (height < 100f) 280f else height
                            }
                            SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING -> {
                                bottomPadding = sharedPreferences.getFloat(SettingsRepository.KEY_KEYBOARD_BOTTOM_PADDING, 0f)
                            }
                            SettingsRepository.KEY_KEYBOARD_ROUNDNESS -> {
                                keyboardRoundness = sharedPreferences.getFloat(SettingsRepository.KEY_KEYBOARD_ROUNDNESS, 24f)
                            }
                            SettingsRepository.KEY_KEYBOARD_SHAPE -> {
                                keyboardShape = sharedPreferences.getInt(SettingsRepository.KEY_KEYBOARD_SHAPE, 0)
                            }
                            SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM -> {
                                isFunctionsBottom = sharedPreferences.getBoolean(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_BOTTOM, false)
                            }
                            SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING -> {
                                functionsPadding = sharedPreferences.getFloat(SettingsRepository.KEY_KEYBOARD_FUNCTIONS_PADDING, 0f)
                            }
                            SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED -> {
                                isHapticsEnabled = sharedPreferences.getBoolean(SettingsRepository.KEY_KEYBOARD_HAPTICS_ENABLED, true)
                            }
                            SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH -> {
                                hapticStrength = sharedPreferences.getFloat(SettingsRepository.KEY_KEYBOARD_HAPTIC_STRENGTH, 0.5f)
                            }
                            SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK -> {
                                isAlwaysDark = sharedPreferences.getBoolean(SettingsRepository.KEY_KEYBOARD_ALWAYS_DARK, false)
                            }
                            SettingsRepository.KEY_KEYBOARD_PITCH_BLACK -> {
                                isPitchBlack = sharedPreferences.getBoolean(SettingsRepository.KEY_KEYBOARD_PITCH_BLACK, false)
                            }
                            SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED -> {
                                isKeyboardClipboardEnabled = sharedPreferences.getBoolean(SettingsRepository.KEY_KEYBOARD_CLIPBOARD_ENABLED, true)
                                if (isKeyboardClipboardEnabled) {
                                    if (::clipboardManager.isInitialized) {
                                        clipboardManager.addPrimaryClipChangedListener(this@EssentialsInputMethodService)
                                        updateClipboardHistory()
                                    }
                                } else {
                                    if (::clipboardManager.isInitialized) {
                                        clipboardManager.removePrimaryClipChangedListener(this@EssentialsInputMethodService)
                                    }
                                }
                            }
                        }
                    }
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                val useDarkTheme = isAlwaysDark || androidx.compose.foundation.isSystemInDarkTheme()
                val suggestions by suggestionEngine.suggestions.collectAsState()

                EssentialsTheme(
                    darkTheme = useDarkTheme,
                    pitchBlackTheme = isPitchBlack
                ) {
                    KeyboardInputView(
                        keyboardHeight = keyboardHeight.dp,
                    bottomPadding = bottomPadding.dp,
                    keyRoundness = keyboardRoundness.dp,
                    keyboardShape = keyboardShape,
                    isHapticsEnabled = isHapticsEnabled,
                    hapticStrength = hapticStrength,
                    isFunctionsBottom = isFunctionsBottom,
                    functionsPadding = functionsPadding.dp,
                    isClipboardEnabled = isKeyboardClipboardEnabled,
                    suggestions = suggestions,
                    clipboardHistory = _clipboardHistory.collectAsState().value,
                    onSuggestionClick = { word ->
                        val ic = currentInputConnection
                        if (ic != null) {
                            val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
                            val lastWord = textBefore.split(Regex("\\s+")).lastOrNull() ?: ""
                            if (lastWord.isNotEmpty()) {
                                ic.deleteSurroundingText(lastWord.length, 0)
                            }
                            ic.commitText(word + " ", 1)
                            suggestionEngine.clearSuggestions()
                        }
                    },
                    onType = { text ->
                        currentInputConnection?.commitText(text, 1)
                    },
                    onPasteClick = { text ->
                        currentInputConnection?.commitText(text, 1)
                    },
                    onUndoClick = {
                        val ic = currentInputConnection
                        if (ic != null && undoStack.isNotEmpty()) {
                            val textToRestore = undoStack.pop()
                            ic.commitText(textToRestore, 1)
                        }
                    },
                    onKeyPress = { keyCode ->
                        handleKeyPress(keyCode)
                    }
                )
                    }
            }
        }
        composedInputView = view
        return view
    }

    override fun onBindInput() {
        super.onBindInput()
        // Also set lifecycle owners on the window decor view if available
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Ensure lifecycle is in RESUMED state when view becomes visible
        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        // Refresh clipboard on show
        updateClipboardHistory()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::clipboardManager.isInitialized) {
            clipboardManager.removePrimaryClipChangedListener(this)
        }
    }

    private fun handleKeyPress(keyCode: Int) {
        val inputConnection = currentInputConnection ?: return
        
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                val selectedText = inputConnection.getSelectedText(0)
                if (selectedText != null && selectedText.isNotEmpty()) {
                    // Selection deleted -> always push as new entry
                    undoStack.push(selectedText.toString())
                    inputConnection.commitText("", 1)
                } else {
                    val before = inputConnection.getTextBeforeCursor(1, 0)
                    if (!before.isNullOrEmpty()) {
                        val char = before[0]
                        val isWhitespace = char.isWhitespace()
                        
                        if (undoStack.isNotEmpty()) {
                            val top = undoStack.peek()
                            // Check if we should merge with the top of the stack
                            // We merge if both are NOT whitespace (building a word)
                            // If either is whitespace, we treat it as a separator and start a new chunk
                            val topIsWhitespace = top?.all { it.isWhitespace() } == true
                            
                            if (!isWhitespace && !topIsWhitespace) {
                                // Merge: Prepend captured char to top
                                val merged = char + undoStack.pop()
                                undoStack.push(merged)
                            } else {
                                // Start new entry
                                undoStack.push(char.toString())
                            }
                        } else {
                            undoStack.push(char.toString())
                        }
                    }
                    inputConnection.deleteSurroundingText(1, 0)
                }
            }
            else -> {
                sendDownUpKeyEvents(keyCode)
            }
        }
    }

    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        val inputView = this.composedInputView ?: return
        
        // Default behavior: contentTopInsets is the top of the input view (relative to window)
        // If we are in "Inverse" shape (2), we have extra top padding equal to roundness.
        // We want the app to ignore this padding, so we lower the contentTopInsets.
        
        if (currentKeyboardShape == 2) {
            val density = resources.displayMetrics.density
            val extraPaddingPx = (currentKeyboardRoundness * density).toInt()
            
            // Allow the app to draw behind the "horns" area (the top padding)
            // By moving the content inset down by the padding amount
            val visibleHeight = inputView.height - extraPaddingPx
            if (visibleHeight > 0) {
                outInsets.contentTopInsets = outInsets.contentTopInsets + extraPaddingPx
                outInsets.visibleTopInsets = outInsets.visibleTopInsets + extraPaddingPx
            }
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        
        // Lookup suggestion for current word
        if (newSelStart == newSelEnd) {
             val ic = currentInputConnection
             if (ic != null) {
                  val textBefore = ic.getTextBeforeCursor(50, 0)?.toString()
                  if (!textBefore.isNullOrEmpty()) {
                       val lastWord = textBefore.split(Regex("\\s+")).lastOrNull() ?: ""
                       // Run lookup
                       if (lastWord.isNotEmpty() && lastWord.all { it.isLetter() }) {
                           lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                                suggestionEngine.lookup(lastWord)
                           }
                       } else {
                           suggestionEngine.clearSuggestions()
                       }
                  } else {
                       suggestionEngine.clearSuggestions()
                  }
             }
        }
    }
}
