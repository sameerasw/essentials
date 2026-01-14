package com.sameerasw.essentials.ui.ime

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp


enum class ShiftState {
    OFF,
    ON,
    LOCKED
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

    Box(
        modifier = modifier
            .bounceClick(interactionSource)
            .clip(shape)
            .background(animatedContainerColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        onPress()
                        tryAwaitRelease()
                        interactionSource.emit(PressInteraction.Release(press))
                    },
                    onTap = { onClick() },
                    onLongPress = if (onLongClick != null) { { onLongClick() } } else null
                )
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
             androidx.compose.material3.LocalContentColor provides animatedContentColor,
             content = content
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KeyboardInputView(
    keyboardHeight: Dp = 54.dp,
    bottomPadding: Dp = 0.dp,
    keyRoundness: Dp = 24.dp,
    isHapticsEnabled: Boolean = true,
    hapticStrength: Float = 0.5f,
    isFunctionsBottom: Boolean = false,
    functionsPadding: Dp = 0.dp,
    onType: (String) -> Unit,
    onKeyPress: (Int) -> Unit
) {
    val view = LocalView.current
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

    var isSymbols by remember { mutableStateOf(false) }
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }



    val keyHeight = keyboardHeight
    val CustomFontFamily = FontFamily(Font(R.font.google_sans_flex))

    // Layers
    val numberRow = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    
    val row1Letters = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2Letters = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3Letters = listOf("z", "x", "c", "v", "b", "n", "m")
    
    val row1Symbols = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
    val row2Symbols = listOf("-", "_", "+", "=", "[", "]", "{", "}", "\\", "|")
    // Adjusted row 3 symbols (8 items to roughly match letter row width when no shift)
    val row3Symbols = listOf(";", ":", "'", "\"", ",", ".", "<", ">") 

    val currentRow1 = if (isSymbols) row1Symbols else row1Letters
    val currentRow2 = if (isSymbols) row2Symbols else row2Letters
    val currentRow3 = if (isSymbols) row3Symbols else row3Letters

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(bottom = bottomPadding, start = 6.dp, end = 6.dp, top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val FunctionRow = @Composable {
            ButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyHeight * 0.5f)
                    .padding(horizontal = functionsPadding),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    val functions = listOf(
                        R.drawable.ic_emoji to "Emoji",
                        R.drawable.ic_clipboard to "Clipboard",
                        R.drawable.ic_undo to "Undo"
                    )
                    
                    functions.forEach { (iconRes, desc) ->
                        val fnInteraction = remember { MutableInteractionSource() }
                        val isPressed by fnInteraction.collectIsPressedAsState()
                        val animatedRadius by animateDpAsState(targetValue = if (isPressed) 4.dp else keyRoundness, label = "cornerRadius")
                        
                        KeyButton(
                            onClick = { },
                            onPress = { performLightHaptic() },
                            interactionSource = fnInteraction,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(animatedRadius),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = desc,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }

        if (!isFunctionsBottom) {
            FunctionRow()
            // Add extra spacing when functions are at top to separate from number row
            // Spacer(modifier = Modifier.height(2.dp)) 
        }

        // Dedicated Number Row
        ButtonGroup(
            modifier = Modifier
                .fillMaxWidth()
                .height(keyHeight), // Slightly compact
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                numberRow.forEach { char ->
                    val numInteraction = remember { MutableInteractionSource() }
                    val isPressed by numInteraction.collectIsPressedAsState()
                    val animatedRadius by animateDpAsState(targetValue = if (isPressed) 4.dp else keyRoundness, label = "cornerRadius")
                    KeyButton(
                        onClick = { onType(char) },
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
        )

        // Row 1
        ButtonGroup(
            modifier = Modifier
                .fillMaxWidth()
                .height(keyHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                currentRow1.forEach { char ->
                    val displayLabel = if (shiftState != ShiftState.OFF && !isSymbols) char.uppercase() else char
                    val row1Interaction = remember { MutableInteractionSource() }
                    val isPressed by row1Interaction.collectIsPressedAsState()
                    val animatedRadius by animateDpAsState(targetValue = if (isPressed) 4.dp else keyRoundness, label = "cornerRadius")
                    KeyButton(
                        onClick = {
                            onType(displayLabel)
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
        )

        // Row 2
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(keyHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!isSymbols) Spacer(modifier = Modifier.weight(0.5f))
            
            ButtonGroup(
                modifier = Modifier.weight(currentRow2.size.toFloat()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    currentRow2.forEach { char ->
                        val displayLabel = if (shiftState != ShiftState.OFF && !isSymbols) char.uppercase() else char
                        val row2Interaction = remember { MutableInteractionSource() }
                        val isPressed by row2Interaction.collectIsPressedAsState()
                        val animatedRadius by animateDpAsState(targetValue = if (isPressed) 4.dp else keyRoundness, label = "cornerRadius")
                        KeyButton(
                            onClick = {
                                onType(displayLabel)
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
            )
            
            if (!isSymbols) Spacer(modifier = Modifier.weight(0.5f))
        }

        // Row 3 (with Shift/Backspace logic)
        ButtonGroup(
            modifier = Modifier
                .fillMaxWidth()
                .height(keyHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                // Shift Key - Only show if not in symbols mode
                if (!isSymbols) {
                    val shiftInteraction = remember { MutableInteractionSource() }
                    val isPressed by shiftInteraction.collectIsPressedAsState()
                    val animatedRadius by animateDpAsState(targetValue = if (isPressed) 4.dp else keyRoundness, label = "cornerRadius")

                    KeyButton(
                        onClick = {
                            shiftState = if (shiftState == ShiftState.OFF) ShiftState.ON else ShiftState.OFF
                        },
                        onPress = { performLightHaptic() },
                        onLongClick = {
                            performHeavyHaptic()
                            shiftState = ShiftState.LOCKED
                        },
                        interactionSource = shiftInteraction,
                        containerColor = if (shiftState != ShiftState.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (shiftState != ShiftState.OFF) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(animatedRadius),
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.key_shift),
                            contentDescription = "Shift",
                            modifier = Modifier.size(24.dp),
                            tint = if (shiftState != ShiftState.OFF) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                     // Spacing balance for symbols mode
                     Spacer(modifier = Modifier.weight(0.5f))
                }

                currentRow3.forEach { char ->
                    val displayLabel = if (shiftState != ShiftState.OFF && !isSymbols) char.uppercase() else char
                    val row3Interaction = remember { MutableInteractionSource() }
                    val isPressed by row3Interaction.collectIsPressedAsState()
                    val animatedRadius by animateDpAsState(targetValue = if (isPressed) 4.dp else keyRoundness, label = "cornerRadius")
                    KeyButton(
                        onClick = {
                            onType(displayLabel)
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

                // Backspace Key
                val backspaceInteraction = remember { MutableInteractionSource() }
                val isPressedDel by backspaceInteraction.collectIsPressedAsState()
                val animatedRadiusDel by animateDpAsState(targetValue = if (isPressedDel) 4.dp else keyRoundness, label = "cornerRadius")
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
                                        val steps = (kotlin.math.abs(delAccumulatedDx) / delSweepThreshold).toInt()
                                        repeat(steps) {
                                            performLightHaptic()
                                            onKeyPress(KeyEvent.KEYCODE_DEL)
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
                                    backspaceInteraction.emit(press)
                                    performLightHaptic()
                                    tryAwaitRelease()
                                    backspaceInteraction.emit(PressInteraction.Release(press))
                                },
                                onTap = {
                                    onKeyPress(KeyEvent.KEYCODE_DEL)
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
                .fillMaxWidth()
                .height(keyHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                // Symbols Toggle
                val symInteraction = remember { MutableInteractionSource() }
                val isPressedSym by symInteraction.collectIsPressedAsState()
                val animatedRadiusSym by animateDpAsState(targetValue = if (isPressedSym) 4.dp else keyRoundness, label = "cornerRadius")
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
                val animatedRadiusComma by animateDpAsState(targetValue = if (isPressedComma) 4.dp else keyRoundness, label = "cornerRadius")
                KeyButton(
                    onClick = { onType(",") },
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
                val animatedRadiusSpace by animateDpAsState(targetValue = if (isPressedSpace) 4.dp else keyRoundness, label = "cornerRadius")
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
                                        val keycode = if (accumulatedDx > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                                        repeat(steps) {
                                            performLightHaptic()
                                            onKeyPress(keycode)
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
                                    onType(" ")
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
                val animatedRadiusDot by animateDpAsState(targetValue = if (isPressedDot) 4.dp else keyRoundness, label = "cornerRadius")
                KeyButton(
                    onClick = { onType(".") },
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
                val animatedRadiusReturn by animateDpAsState(targetValue = if (isPressedReturn) 4.dp else keyRoundness, label = "cornerRadius")
                KeyButton(
                    onClick = { onKeyPress(KeyEvent.KEYCODE_ENTER) },
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

        if (isFunctionsBottom) {
            FunctionRow()
        }
    }
}
