package com.sameerasw.essentials.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    onSwipeDownToExit: () -> Unit = {},
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
    val gridStates = remember { mutableStateMapOf<Int, LazyGridState>() }

    fun performHaptic(strength: Float) {
        if (isHapticsEnabled) {
            HapticUtil.performCustomHaptic(view, strength)
        }
    }

    // Haptic feedback on category switch
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != -1) {
            performHaptic(hapticStrength * 0.5f)
        }
    }

    // Nested Scroll for swipe-down exit gesture
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (pagerState.currentPage == 0 && available.y > 50f) {
                    val gridState = gridStates[0]
                    if (gridState?.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0) {
                        onSwipeDownToExit()
                        return available
                    }
                }
                return Offset.Zero
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .nestedScroll(nestedScrollConnection)
    ) {
        // Emoji Grid Area (Left side)
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            beyondViewportPageCount = 1,
            userScrollEnabled = true,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = 0.15f
            )
        ) { pageIndex ->
            val category = EmojiData.categories.getOrNull(pageIndex)
            if (category != null) {
                val gridState = gridStates.getOrPut(pageIndex) { LazyGridState() }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category Header within the page
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp, start = 12.dp)
                    )

                    LazyVerticalGrid(
                        state = gridState,
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
                            key = { "${category.name}_${it.name}_${it.emoji}" },
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

        // Vertical Category Rail (Right side)
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmojiData.categories.forEachIndexed { index, category ->
                val isSelected = index == pagerState.currentPage
                val interactionSource = remember { MutableInteractionSource() }
                
                IconButton(
                    onClick = {
                        scope.launch {
                            // Reset scroll position of the target category grid
                            gridStates[index]?.scrollToItem(0)
                            pagerState.animateScrollToPage(index)
                        }
                        performHaptic(hapticStrength * 0.8f)
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(vertical = 2.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
