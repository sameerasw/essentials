package com.sameerasw.essentials.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    modifier: Modifier = Modifier,
    keyRoundness: Dp = 24.dp,
    isHapticsEnabled: Boolean = true,
    hapticStrength: Float = 0.5f,
    onEmojiSelected: (String) -> Unit,
    bottomContentPadding: Dp = 0.dp
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    if (EmojiData.categories.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { EmojiData.categories.size })

    fun performHaptic(strength: Float) {
        if (isHapticsEnabled) {
            HapticUtil.performCustomHaptic(view, strength)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Category Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentPage by remember { derivedStateOf { pagerState.currentPage } }
            EmojiData.categories.forEachIndexed { index, category ->
                val isSelected = index == currentPage
                val interactionSource = remember { MutableInteractionSource() }
                
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                        performHaptic(hapticStrength * 0.8f)
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(keyRoundness / 2))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        painter = painterResource(id = category.iconRes),
                        contentDescription = category.name,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Horizontal Pager for Category Swiping
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val category = EmojiData.categories.getOrNull(pageIndex)
            if (category != null) {
                // Emoji Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    contentPadding = PaddingValues(bottom = bottomContentPadding + 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = category.emojis,
                        key = { it.name + it.emoji },
                        contentType = { "emoji" }
                    ) { emojiObj ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(keyRoundness))
                                .background(
                                    if (isPressed) MaterialTheme.colorScheme.surfaceContainerHighest 
                                    else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = {
                                        onEmojiSelected(emojiObj.emoji)
                                        performHaptic(hapticStrength)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emojiObj.emoji,
                                fontSize = 28.sp,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
