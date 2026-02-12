package com.sameerasw.essentials.ui.ime

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


enum class ShiftState {
    OFF,
    ON,
    LOCKED
}

class LiquidShape(private val curveHeight: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            // Left Horn
            moveTo(0f, 0f)
            // Curve down from (0,0) to (R, R) with control point at (0, R)
            // This creates a vertical tangent at the wall and horizontal at the bottom
            quadraticTo(0f, curveHeight, curveHeight, curveHeight)

            // Flat bottom of the meniscus
            lineTo(size.width - curveHeight, curveHeight)

            // Right Horn
            // Curve up from (W-R, R) to (W, 0) with control point at (W, R)
            quadraticTo(size.width, curveHeight, size.width, 0f)

            // Rest of the box
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

private fun Modifier.bounceClick(interactionSource: MutableInteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.9f else 1f, label = "scale")
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun KeyButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPress: () -> Unit = {}, // For Haptics/Anim
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource,
    shape: androidx.compose.ui.graphics.Shape,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primaryContainer else containerColor,
        label = "ButtonContainerColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
        label = "ButtonContentColor"
    )

    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val viewConfiguration = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .bounceClick(interactionSource)
            .clip(shape)
            .background(animatedContainerColor)
            .pointerInput(onClick, onLongClick, onPress) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val press = PressInteraction.Press(down.position)
                        onPress()
                        scope.launch { interactionSource.emit(press) }

                        var released = false
                        var isLongClickTriggered = false

                        val longPressJob = onLongClick?.let {
                            scope.launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                if (!released) {
                                    isLongClickTriggered = true
                                    it()
                                    HapticUtil.performHeavyHaptic(view)
                                }
                            }
                        }

                        // Wait for up or cancel of THIS specific pointer
                        var up: PointerInputChange? = null
                        var isCanceledByMovement = false
                        while (up == null) {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == down.id }
                            if (change == null || change.isConsumed) {
                                // Canceled or lost tracker
                                break
                            } else {
                                // Check if user moved too far (swiped)
                                val distance = (change.position - down.position).getDistance()
                                if (distance > viewConfiguration.touchSlop) {
                                    isCanceledByMovement = true
                                }
                                
                                if (change.changedToUp()) {
                                    up = change
                                }
                            }
                        }

                        released = true
                        longPressJob?.cancel()

                        if (up != null) {
                            scope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                            if (!isLongClickTriggered && !isCanceledByMovement) {
                                onClick()
                            }
                        } else {
                            scope.launch { interactionSource.emit(PressInteraction.Cancel(press)) }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides animatedContentColor,
            content = content
        )
    }
}

@Composable
fun ClipboardItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "ClipboardItemColor"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(animatedColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KeyboardInputView(
    keyboardHeight: Dp = 280.dp,
    bottomPadding: Dp = 0.dp,
    keyRoundness: Dp = 24.dp,
    keyboardShape: Int = 0, // 0=Round, 1=Flat, 2=Inverse
    isHapticsEnabled: Boolean = true,
    hapticStrength: Float = 0.5f,
    isFunctionsBottom: Boolean = false,
    functionsPadding: Dp = 0.dp,
    isClipboardEnabled: Boolean = true,
    suggestions: List<String> = emptyList(),
    clipboardHistory: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    onPasteClick: (String) -> Unit = {},
    onUndoClick: () -> Unit = {},
    onType: (String) -> Unit,
    onKeyPress: (Int) -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var isSymbols by remember { mutableStateOf(false) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var isClipboardMode by remember { mutableStateOf(false) }
    var isEmojiMode by remember { mutableStateOf(false) }
    var isSuggestionsCollapsed by remember { mutableStateOf(false) }
    var currentWord by remember { mutableStateOf("") }

    val emojiCandidates = remember(currentWord) {
        if (currentWord.length >= 3) {
            EmojiData.allEmojis
                .filter { it.name.contains(currentWord, ignoreCase = true) }
                .take(5)
                .map { it.emoji }
        } else {
            emptyList()
        }
    }

    val mergedSuggestions = remember(suggestions, emojiCandidates) {
        if (emojiCandidates.isNotEmpty() && suggestions.isNotEmpty()) {
            // Priority: Text 1 -> Emoji 1 -> Remaining Text -> Remaining Emojis
            listOf(suggestions[0]) + emojiCandidates.take(1) + suggestions.drop(1) + emojiCandidates.drop(1)
        } else {
            emojiCandidates + suggestions
        }
    }

    fun handleType(text: String) {
        onType(text)
        if (text.length == 1 && (text[0].isLetterOrDigit() || text[0] == '\'')) {
            currentWord += text
        } else {
            currentWord = ""
        }
    }

    fun handleKeyPress(keyCode: Int) {
        onKeyPress(keyCode)
        if (keyCode == android.view.KeyEvent.KEYCODE_DEL) {
            if (currentWord.isNotEmpty()) {
                currentWord = currentWord.dropLast(1)
            }
        } else {
            currentWord = ""
        }
    }

    // Total Height animation
    val animatedTotalHeight by animateDpAsState(
        targetValue = if (isEmojiMode) keyboardHeight + 120.dp else keyboardHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "TotalHeightAnimation"
    )
    val totalHeight = animatedTotalHeight

    // Pre-load Emoji data on startup (Background thread)
    LaunchedEffect(Unit) {
        EmojiData.load(view.context, scope)
    }

    fun performLightHaptic() {
        if (isHapticsEnabled) {
            HapticUtil.performCustomHaptic(view, hapticStrength)
        }
    }

    fun performHeavyHaptic() {
        if (isHapticsEnabled) {
            HapticUtil.performHeavyHaptic(view)
        }
    }

    fun performScrollHaptic() {
        if (isHapticsEnabled) {
            HapticUtil.performCustomHaptic(view, hapticStrength * 0.4f)
        }
    }

    val CustomFontFamily = remember { FontFamily(Font(R.font.google_sans_flex)) }

    // Layers
    val numberRow = remember { listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0") }

    val row1Letters = remember { listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p") }
    val row2Letters = remember { listOf("a", "s", "d", "f", "g", "h", "j", "k", "l") }
    val row3Letters = remember { listOf("z", "x", "c", "v", "b", "n", "m") }

    val row1Symbols = remember { listOf("~", "\\", "|", "^", "<", ">", "[", "]", "{", "}") }
    val row2Symbols = remember { listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/") }
    val row3Symbols = remember { listOf("*", "\"", "'", ":", ";", "!", "?") }

    val currentRow1 = if (isSymbols) row1Symbols else row1Letters
    val currentRow2 = if (isSymbols) row2Symbols else row2Letters
    val currentRow3 = if (isSymbols) row3Symbols else row3Letters

    val density = LocalDensity.current
    val containerShape = remember(keyboardShape, keyRoundness) {
        when (keyboardShape) {
            1 -> androidx.compose.ui.graphics.RectangleShape
            2 -> LiquidShape(with(density) { keyRoundness.toPx() }) // Inverse/Liquid
            else -> RoundedCornerShape(28.dp)
        }
    }

    val extraTopPadding = remember(keyboardShape, keyRoundness) {
        if (keyboardShape == 2) keyRoundness else 0.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    if (!isEmojiMode && dragAmount < -20f) { // Swipe up
                        isEmojiMode = true
                        isClipboardMode = false
                        performHeavyHaptic()
                    }
                }
            }
            .padding(
                bottom = if (isEmojiMode) 0.dp else bottomPadding,
                start = 6.dp,
                end = 6.dp,
                top = 6.dp + extraTopPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val FunctionRow: @Composable (Modifier) -> Unit = { modifier ->
            val hasSuggestions = mergedSuggestions.isNotEmpty()
            val showSuggestions = hasSuggestions && !isEmojiMode && !isSuggestionsCollapsed

            val rotation by animateFloatAsState(
                targetValue = if (showSuggestions) 45f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "controlIconRotation"
            )

            AnimatedContent(
                targetState = showSuggestions,
                transitionSpec = {
                    val springSpec = spring<IntOffset>(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                    if (targetState) {
                        // Expand
                        (fadeIn() + slideInHorizontally(animationSpec = springSpec) { it })
                            .togetherWith(fadeOut() + slideOutHorizontally(animationSpec = springSpec) { -it })
                    } else {
                        // Collapse
                        (fadeIn() + slideInHorizontally(animationSpec = springSpec) { -it })
                            .togetherWith(fadeOut() + slideOutHorizontally(animationSpec = springSpec) { it })
                    }
                },
                label = "FunctionRowTransition",
                modifier = modifier
            ) { targetShowSuggestions ->
                if (targetShowSuggestions) {
                    val collapseInteraction = remember { MutableInteractionSource() }
                    val carouselState = rememberCarouselState { mergedSuggestions.count() }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val nestedScrollConnection = remember {
                            object : NestedScrollConnection {
                                var accumulatedScroll = 0f
                                val threshold = 70f

                                override fun onPreScroll(
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    if (source == NestedScrollSource.UserInput) {
                                        accumulatedScroll += available.x
                                        if (kotlin.math.abs(accumulatedScroll) >= threshold) {
                                            performScrollHaptic()
                                            accumulatedScroll = 0f
                                        }
                                    }
                                    return Offset.Zero
                                }
                            }
                        }

                        HorizontalMultiBrowseCarousel(
                            state = carouselState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .nestedScroll(nestedScrollConnection),
                            preferredItemWidth = 150.dp,
                            itemSpacing = 4.dp,
                            minSmallItemWidth = 10.dp,
                            maxSmallItemWidth = 20.dp,
                            contentPadding = PaddingValues(start = functionsPadding)
                        ) { i ->
                            val suggestion = mergedSuggestions[i]
                            val suggInteraction = remember { MutableInteractionSource() }
                            val animatedRadius by animateDpAsState(
                                targetValue = keyRoundness,
                                label = "cornerRadius"
                            )

                            KeyButton(
                                onClick = { 
                                    onSuggestionClick(suggestion)
                                    // If it's an emoji (single char usually, or check length), don't add space if app does, 
                                    // but we just pass it out. Let's reset currentWord if it's a COMMIT.
                                    currentWord = "" 
                                },
                                onPress = { performLightHaptic() },
                                interactionSource = suggInteraction,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(animatedRadius),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .maskClip(RoundedCornerShape(animatedRadius))
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CustomFontFamily,
                                    maxLines = 1
                                )
                            }
                        }

                        // Collapse Button (Far Right)
                        val isCollapsePressed by collapseInteraction.collectIsPressedAsState()
                        val collapseRadius by animateDpAsState(
                            targetValue = if (isCollapsePressed) 4.dp else keyRoundness,
                            label = "collapseRadius"
                        )

                        KeyButton(
                            onClick = { 
                                isSuggestionsCollapsed = true 
                                performLightHaptic()
                            },
                            onPress = { performLightHaptic() },
                            interactionSource = collapseInteraction,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(collapseRadius),
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(50.dp)
                                .padding(end = functionsPadding)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_add_24),
                                contentDescription = "Collapse Suggestions",
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                } else {
                    ButtonGroup(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = functionsPadding),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        content = {
                            val functions = remember(isClipboardEnabled, isEmojiMode, isSuggestionsCollapsed, hasSuggestions) {
                                val list = mutableListOf(
                                    R.drawable.ic_emoji to "Emoji",
                                    if (isEmojiMode) R.drawable.rounded_backspace_24 to "Backspace" 
                                    else R.drawable.ic_undo to "Undo"
                                )
                                if (isClipboardEnabled) {
                                    list.add(1, R.drawable.ic_clipboard to "Clipboard")
                                }
                                // Add Expand button if collapsed and suggestions exist
                                if (isSuggestionsCollapsed && hasSuggestions && !isEmojiMode) {
                                    list.add(R.drawable.rounded_add_24 to "Expand")
                                }
                                list
                            }

                            functions.forEach { (iconRes, desc) ->
                                val fnInteraction = remember { MutableInteractionSource() }
                                val isPressed by fnInteraction.collectIsPressedAsState()
                                val animatedRadius by animateDpAsState(
                                    targetValue = if (isPressed) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )

                                KeyButton(
                                    onClick = {
                                        if (desc == "Clipboard") {
                                            isClipboardMode = !isClipboardMode
                                            if (isClipboardMode) isEmojiMode = false
                                        } else if (desc == "Undo") {
                                            onUndoClick()
                                        } else if (desc == "Emoji") {
                                            isEmojiMode = !isEmojiMode
                                            if (isEmojiMode) isClipboardMode = false
                                        } else if (desc == "Backspace") {
                                            onKeyPress(android.view.KeyEvent.KEYCODE_DEL)
                                        } else if (desc == "Expand") {
                                            isSuggestionsCollapsed = false
                                        }
                                    },
                                    onPress = { performLightHaptic() },
                                    interactionSource = fnInteraction,
                                    containerColor = if ((desc == "Clipboard" && isClipboardMode) || (desc == "Emoji" && isEmojiMode)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = if ((desc == "Clipboard" && isClipboardMode) || (desc == "Emoji" && isEmojiMode)) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(animatedRadius),
                                    modifier = if (desc == "Expand") {
                                        Modifier.width(50.dp).fillMaxHeight()
                                    } else {
                                        Modifier.weight(1.3f).fillMaxHeight()
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = desc,
                                        modifier = Modifier
                                            .size(if (desc == "Expand") 18.dp else 20.dp)
                                            .then(
                                                if (desc == "Expand") {
                                                    Modifier.graphicsLayer { rotationZ = rotation }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        if (!isFunctionsBottom) {
            FunctionRow(
                Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            )
        }

        val currentMode = when {
            isEmojiMode -> 1
            isClipboardMode && isClipboardEnabled -> 2
            else -> 0
        }

        AnimatedContent(
            targetState = currentMode,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(5f),
            label = "KeyboardModeAnimation"
        ) { mode ->
            when (mode) {
                2 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(keyRoundness))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(8.dp)
                    ) {
                        if (clipboardHistory.isEmpty()) {
                            Text(
                                text = "Clipboard is empty",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(clipboardHistory) { clipText ->
                                    ClipboardItem(
                                        text = clipText,
                                        shape = RoundedCornerShape(keyRoundness),
                                        onClick = {
                                            onPasteClick(clipText)
                                            isClipboardMode = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    EmojiPicker(
                        modifier = Modifier.fillMaxSize(),
                        keyRoundness = keyRoundness,
                        isHapticsEnabled = isHapticsEnabled,
                        hapticStrength = hapticStrength,
                        onEmojiSelected = { emoji ->
                            handleType(emoji)
                        },
                        onSwipeDownToExit = {
                            if (isEmojiMode) {
                                isEmojiMode = false
                                performHeavyHaptic()
                            }
                        },
                        bottomContentPadding = bottomPadding
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Dedicated Number Row
                        ButtonGroup(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            content = {
                                numberRow.forEach { char ->
                                    key(char) {
                                        val numInteraction = remember { MutableInteractionSource() }
                                        val isPressed by numInteraction.collectIsPressedAsState()
                                        KeyButton(
                                            onClick = { handleType(char) },
                                            onPress = { performLightHaptic() },
                                            interactionSource = numInteraction,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(keyRoundness),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            Text(
                                                text = char,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = CustomFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        // Row 1
                        ButtonGroup(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            content = {
                                currentRow1.forEach { char ->
                                    key(char) {
                                        val displayLabel =
                                            if (shiftState != ShiftState.OFF && !isSymbols) char.uppercase() else char
                                        val row1Interaction = remember { MutableInteractionSource() }
                                        val isPressed by row1Interaction.collectIsPressedAsState()
                                        val animatedRadius by animateDpAsState(
                                            targetValue = if (isPressed) 4.dp else keyRoundness,
                                            label = "cornerRadius"
                                        )
                                        KeyButton(
                                            onClick = {
                                                handleType(displayLabel)
                                                if (shiftState == ShiftState.ON) shiftState = ShiftState.OFF
                                            },
                                            onPress = { performLightHaptic() },
                                            interactionSource = row1Interaction,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(animatedRadius),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            Text(
                                                text = displayLabel,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFamily = CustomFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        // Row 2
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!isSymbols) Spacer(modifier = Modifier.weight(0.5f))

                            ButtonGroup(
                                modifier = Modifier.weight(currentRow2.size.toFloat()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                content = {
                                    currentRow2.forEach { char ->
                                        key(char) {
                                            val displayLabel =
                                                if (shiftState != ShiftState.OFF && !isSymbols) char.uppercase() else char
                                            val row2Interaction = remember { MutableInteractionSource() }
                                            val isPressed by row2Interaction.collectIsPressedAsState()
                                            val animatedRadius by animateDpAsState(
                                                targetValue = if (isPressed) 4.dp else keyRoundness,
                                                label = "cornerRadius"
                                            )
                                            KeyButton(
                                                onClick = {
                                                    handleType(displayLabel)
                                                    if (shiftState == ShiftState.ON) shiftState = ShiftState.OFF
                                                },
                                                onPress = { performLightHaptic() },
                                                interactionSource = row2Interaction,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                                shape = RoundedCornerShape(animatedRadius),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            ) {
                                                Text(
                                                    text = displayLabel,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = CustomFontFamily
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            if (!isSymbols) Spacer(modifier = Modifier.weight(0.5f))
                        }

                        // Row 3 (with Shift/Backspace logic)
                        ButtonGroup(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            content = {
                                // Shift Key
                                val shiftInteraction = remember { MutableInteractionSource() }
                                val isPressed by shiftInteraction.collectIsPressedAsState()
                                val animatedRadius by animateDpAsState(
                                    targetValue = if (isPressed) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )

                                KeyButton(
                                    onClick = {
                                        if (!isSymbols) {
                                            shiftState =
                                                if (shiftState == ShiftState.OFF) ShiftState.ON else ShiftState.OFF
                                        }
                                    },
                                    onPress = { performLightHaptic() },
                                    onLongClick = {
                                        if (!isSymbols) {
                                            performHeavyHaptic()
                                            shiftState = ShiftState.LOCKED
                                        }
                                    },
                                    interactionSource = shiftInteraction,
                                    containerColor = if (isSymbols) {
                                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                    } else if (shiftState != ShiftState.OFF) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                    contentColor = if (isSymbols) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    } else if (shiftState != ShiftState.OFF) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    shape = RoundedCornerShape(animatedRadius),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .fillMaxHeight()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.key_shift),
                                        contentDescription = "Shift",
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSymbols) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        } else if (shiftState != ShiftState.OFF) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }

                                currentRow3.forEach { char ->
                                    key(char) {
                                        val displayLabel =
                                            if (shiftState != ShiftState.OFF && !isSymbols) char.uppercase() else char
                                        val row3Interaction = remember { MutableInteractionSource() }
                                        val isPressed by row3Interaction.collectIsPressedAsState()
                                        val animatedRadius by animateDpAsState(
                                            targetValue = if (isPressed) 4.dp else keyRoundness,
                                            label = "cornerRadius"
                                        )
                                        KeyButton(
                                            onClick = {
                                                handleType(displayLabel)
                                                if (shiftState == ShiftState.ON) shiftState = ShiftState.OFF
                                            },
                                            onPress = { performLightHaptic() },
                                            interactionSource = row3Interaction,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(animatedRadius),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            Text(
                                                text = displayLabel,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFamily = CustomFontFamily
                                            )
                                        }
                                    }
                                }

                                // Backspace Key
                                val backspaceInteraction = remember { MutableInteractionSource() }
                                val isPressedDel by backspaceInteraction.collectIsPressedAsState()
                                val animatedRadiusDel by animateDpAsState(
                                    targetValue = if (isPressedDel) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )
                                var delAccumulatedDx by remember { mutableStateOf(0f) }
                                val delSweepThreshold = 25f

                                val animatedColorDel by animateColorAsState(
                                    targetValue = if (isPressedDel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                    label = "DelColor"
                                )
                                val animatedContentColorDel by animateColorAsState(
                                    targetValue = if (isPressedDel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    label = "DelContentColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .fillMaxHeight()
                                        .bounceClick(backspaceInteraction)
                                        .clip(RoundedCornerShape(animatedRadiusDel))
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { delAccumulatedDx = 0f },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    delAccumulatedDx += dragAmount
                                                    // Moving left (negative dx) for delete
                                                    if (delAccumulatedDx <= -delSweepThreshold) {
                                                        val steps =
                                                            (kotlin.math.abs(delAccumulatedDx) / delSweepThreshold).toInt()
                                                        repeat(steps) {
                                                            performLightHaptic()
                                                            handleKeyPress(KeyEvent.KEYCODE_DEL)
                                                        }
                                                        delAccumulatedDx %= delSweepThreshold
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = { offset ->
                                                    val press = PressInteraction.Press(offset)
                                                    performLightHaptic()
                                                    scope.launch { backspaceInteraction.emit(press) }
                                                    if (tryAwaitRelease()) {
                                                        scope.launch {
                                                            backspaceInteraction.emit(
                                                                PressInteraction.Release(press)
                                                            )
                                                        }
                                                        handleKeyPress(KeyEvent.KEYCODE_DEL)
                                                    } else {
                                                        scope.launch {
                                                            backspaceInteraction.emit(
                                                                PressInteraction.Cancel(press)
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .background(animatedColorDel),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_backspace_24),
                                        contentDescription = "Backspace",
                                        modifier = Modifier.size(24.dp),
                                        tint = animatedContentColorDel
                                    )
                                }
                            }
                        )

                        // Row 4 (Sym, Space, Return)
                        ButtonGroup(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            content = {
                                // Symbols Toggle
                                val symInteraction = remember { MutableInteractionSource() }
                                val isPressedSym by symInteraction.collectIsPressedAsState()
                                val animatedRadiusSym by animateDpAsState(
                                    targetValue = if (isPressedSym) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )
                                KeyButton(
                                    onClick = { isSymbols = !isSymbols },
                                    onPress = { performLightHaptic() },
                                    interactionSource = symInteraction,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(animatedRadiusSym),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .fillMaxHeight()
                                ) {
                                    Text(
                                        text = if (isSymbols) "ABC" else "?#/",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = CustomFontFamily
                                    )
                                }

                                // Comma Key
                                val commaInteraction = remember { MutableInteractionSource() }
                                val isPressedComma by commaInteraction.collectIsPressedAsState()
                                val animatedRadiusComma by animateDpAsState(
                                    targetValue = if (isPressedComma) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )
                                KeyButton(
                                    onClick = { handleType(",") },
                                    onPress = { performLightHaptic() },
                                    interactionSource = commaInteraction,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(animatedRadiusComma),
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .fillMaxHeight()
                                ) {
                                    Text(
                                        text = ",",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = CustomFontFamily
                                    )
                                }

                                // Space
                                val spaceInteraction = remember { MutableInteractionSource() }
                                val isPressedSpace by spaceInteraction.collectIsPressedAsState()
                                val animatedRadiusSpace by animateDpAsState(
                                    targetValue = if (isPressedSpace) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )
                                var accumulatedDx by remember { mutableStateOf(0f) }
                                val sweepThreshold = 25f // pixels per cursor move

                                val animatedColorSpace by animateColorAsState(
                                    targetValue = if (isPressedSpace) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    label = "SpaceColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(3f)
                                        .fillMaxHeight()
                                        .bounceClick(spaceInteraction)
                                        .clip(RoundedCornerShape(animatedRadiusSpace))
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { accumulatedDx = 0f },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDx += dragAmount
                                                    val absDx = kotlin.math.abs(accumulatedDx)
                                                    if (absDx >= sweepThreshold) {
                                                        val steps = (absDx / sweepThreshold).toInt()
                                                        val keycode =
                                                            if (accumulatedDx > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                                                        repeat(steps) {
                                                            performLightHaptic()
                                                            handleKeyPress(keycode)
                                                        }
                                                        accumulatedDx %= sweepThreshold
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = { offset ->
                                                    val press = PressInteraction.Press(offset)
                                                    spaceInteraction.emit(press)
                                                    performLightHaptic()
                                                    tryAwaitRelease()
                                                    spaceInteraction.emit(PressInteraction.Release(press))
                                                },
                                                onTap = {
                                                    handleType(" ")
                                                }
                                            )
                                        }
                                        .background(animatedColorSpace),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Empty space
                                }

                                // Dot Key
                                val dotInteraction = remember { MutableInteractionSource() }
                                val isPressedDot by dotInteraction.collectIsPressedAsState()
                                val animatedRadiusDot by animateDpAsState(
                                    targetValue = if (isPressedDot) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )
                                KeyButton(
                                    onClick = { handleType(".") },
                                    onPress = { performLightHaptic() },
                                    interactionSource = dotInteraction,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(animatedRadiusDot),
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .fillMaxHeight()
                                ) {
                                    Text(
                                        text = ".",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = CustomFontFamily
                                    )
                                }

                                // Return
                                val returnInteraction = remember { MutableInteractionSource() }
                                val isPressedReturn by returnInteraction.collectIsPressedAsState()
                                val animatedRadiusReturn by animateDpAsState(
                                    targetValue = if (isPressedReturn) 4.dp else keyRoundness,
                                    label = "cornerRadius"
                                )
                                KeyButton(
                                    onClick = { handleKeyPress(KeyEvent.KEYCODE_ENTER) },
                                    onPress = { performLightHaptic() },
                                    interactionSource = returnInteraction,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = RoundedCornerShape(animatedRadiusReturn),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .fillMaxHeight()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_keyboard_return_24),
                                        contentDescription = "Return",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isFunctionsBottom) {
            FunctionRow(
                Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            )
        }
    }
}
