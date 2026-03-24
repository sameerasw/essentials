package com.sameerasw.essentials.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaomojiPicker(
    modifier: Modifier = Modifier,
    keyRoundness: Dp = 24.dp,
    isHapticsEnabled: Boolean = true,
    hapticStrength: Float = 0.5f,
    onKaomojiSelected: (String) -> Unit,
    onSwipeDownToExit: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    if (KaomojiData.categories.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { KaomojiData.categories.size })
    val gridStates = remember { mutableStateMapOf<Int, LazyGridState>() }
    val railScrollState = rememberLazyListState()

    fun performHaptic(strength: Float) {
        if (isHapticsEnabled) {
            HapticUtil.performCustomHaptic(view, strength)
        }
    }

    // Sync rail scroll and haptic with pager
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != -1) {
            railScrollState.animateScrollToItem(pagerState.currentPage)
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
        // Kaomoji Grid
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
            val category = KaomojiData.categories.getOrNull(pageIndex)
            if (category != null) {
                val gridState = gridStates.getOrPut(pageIndex) { LazyGridState() }
                val context = androidx.compose.ui.platform.LocalContext.current
                val categoryNameRes = remember(category.name) {
                    context.resources.getIdentifier("kaomoji_cat_${category.name}", "string", context.packageName)
                }
                val localizedName = if (categoryNameRes != 0) androidx.compose.ui.res.stringResource(categoryNameRes) else category.name.replaceFirstChar { it.uppercase() }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category Header within the page
//                    Text(
//                        text = localizedName,
//                        style = MaterialTheme.typography.labelLarge,
//                        color = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(top = 12.dp, bottom = 4.dp, start = 12.dp)
//                    )

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(bottom = bottomContentPadding + 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(
                            items = category.kaomojis,
                            key = { index, it -> "${category.name}_${it.value}_$index" },
                            contentType = { _, _ -> "kaomoji" }
                        ) { index, kaomojiObj ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(keyRoundness))
                                    .background(
                                        if (isPressed) MaterialTheme.colorScheme.surfaceContainerHighest 
                                        else MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = {
                                            onKaomojiSelected(kaomojiObj.value)
                                            performHaptic(hapticStrength)
                                        }
                                    )
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = kaomojiObj.value,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Vertical Category Rail
        LazyColumn(
            state = railScrollState,
            modifier = Modifier
                .width(65.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(KaomojiData.categories.size) { index ->
                val category = KaomojiData.categories[index]
                val isSelected = index == pagerState.currentPage
                val interactionSource = remember { MutableInteractionSource() }
                
                val context = androidx.compose.ui.platform.LocalContext.current
                val categoryNameRes = remember(category.name) {
                    context.resources.getIdentifier("kaomoji_cat_${category.name}", "string", context.packageName)
                }
                val localizedName = if (categoryNameRes != 0) androidx.compose.ui.res.stringResource(categoryNameRes) else category.name.replaceFirstChar { it.uppercase() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(keyRoundness / 2))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                scope.launch {
                                    gridStates[index]?.scrollToItem(0)
                                    pagerState.animateScrollToPage(index)
                                }
                                performHaptic(hapticStrength * 0.8f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.background
                               else MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .basicMarquee()
                    )
                }
            }
        }
    }
}
