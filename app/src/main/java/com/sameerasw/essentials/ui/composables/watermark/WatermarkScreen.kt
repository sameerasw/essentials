package com.sameerasw.essentials.ui.composables.watermark

import android.graphics.drawable.Icon
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.WatermarkStyle
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.viewmodels.WatermarkUiState
import com.sameerasw.essentials.viewmodels.WatermarkViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WatermarkScreen(
    initialUri: Uri?,
    onPickImage: () -> Unit,
    onBack: () -> Unit,
    viewModel: WatermarkViewModel
) {
    val context = LocalContext.current
    val options by viewModel.options.collectAsState()
    val previewState by viewModel.previewUiState.collectAsState()
    val saveState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            viewModel.loadPreview(initialUri)
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is WatermarkUiState.Success -> {
                Toast.makeText(context, R.string.watermark_save_success, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is WatermarkUiState.Error -> {
                Toast.makeText(context, (saveState as WatermarkUiState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            ReusableTopAppBar(
                title = R.string.feat_watermark_title,
                hasBack = true,
                onBackClick = onBack,
                isSmall = true,
                actions = {
                    val pickImageButton = @Composable {
                         // Pick Image Button
                        if (initialUri == null) {
                            // Primary when no image
                            androidx.compose.material3.Button(onClick = onPickImage) {
                                 Icon(
                                     painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_add_photo_alternate_24),
                                     contentDescription = stringResource(R.string.watermark_pick_image),
                                     modifier = Modifier.size(18.dp)
                                 )
                                 Spacer(Modifier.size(8.dp))
                                 Text(stringResource(R.string.watermark_pick_image))
                            }
                        } else {
                            // Secondary when image is there
                            androidx.compose.material3.OutlinedButton(onClick = onPickImage) {
                                 Icon(
                                     painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_add_photo_alternate_24),
                                     contentDescription = stringResource(R.string.watermark_pick_image),
                                     modifier = Modifier.size(18.dp)
                                 )
                            }
                        }
                    }

                    pickImageButton()

                    // Save/Share Menu Button
                    if (initialUri != null) {
                        Spacer(Modifier.size(8.dp))
                        var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        
                        Box {
                             // Save Button (Primary)
                             androidx.compose.material3.Button(onClick = { showMenu = true }) {
                                 Icon(
                                     painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_save_24),
                                     contentDescription = stringResource(R.string.action_save),
                                     modifier = Modifier.size(18.dp)
                                 )
                                 Spacer(Modifier.size(8.dp))
                                 Text(stringResource(R.string.action_save))
                             }
                             
                             com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
                                 expanded = showMenu,
                                 onDismissRequest = { showMenu = false },
                             ) {
                                 // Share Option
                                 com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                                     text = { Text(stringResource(R.string.action_share)) },
                                     leadingIcon = {
                                         Icon(
                                             painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_share_24),
                                             contentDescription = null,
                                             modifier = Modifier.size(20.dp)
                                         )
                                     },
                                     onClick = {
                                         showMenu = false
                                          initialUri.let { uri ->
                                             viewModel.shareImage(uri) { sharedUri ->
                                                 val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                     type = "image/jpeg"
                                                     putExtra(android.content.Intent.EXTRA_STREAM, sharedUri)
                                                     addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                 }
                                                 context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.action_share)))
                                             }
                                        } 
                                     },
                                     enabled = saveState !is WatermarkUiState.Processing
                                 )
                                 
                                 // Save Option
                                 com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                                     text = { Text(stringResource(R.string.action_save)) },
                                     leadingIcon = {
                                         Icon(
                                             painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_save_24),
                                             contentDescription = null,
                                              modifier = Modifier.size(20.dp)
                                         )
                                     },
                                     onClick = {
                                         showMenu = false
                                         viewModel.saveImage(initialUri)
                                     },
                                     enabled = saveState !is WatermarkUiState.Processing
                                 )
                             }
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { padding ->
        val density = androidx.compose.ui.platform.LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp
        
        val maxPreviewHeightDp = screenHeightDp * 0.6f
        val minPreviewHeightDp = screenHeightDp * 0.4f
        
        val maxPx = with(density) { maxPreviewHeightDp.toPx() }
        val minPx = with(density) { minPreviewHeightDp.toPx() }
        
        var previewHeightPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(maxPx) }
        
        val nestedScrollConnection = androidx.compose.runtime.remember {
            object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    val delta = available.y
                    // Swiping Up (delta < 0): Collapse
                    if (delta < 0) {
                        val newHeight = (previewHeightPx + delta).coerceIn(minPx, maxPx)
                        val consumed = newHeight - previewHeightPx
                        previewHeightPx = newHeight
                        return androidx.compose.ui.geometry.Offset(0f, consumed)
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }

                override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    val delta = available.y
                    // Swiping Down (delta > 0): Expand
                    if (delta > 0) {
                        val newHeight = (previewHeightPx + delta).coerceIn(minPx, maxPx)
                        val consumedY = newHeight - previewHeightPx
                        previewHeightPx = newHeight
                        return androidx.compose.ui.geometry.Offset(0f, consumedY)
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Preview Area (Variable Height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { previewHeightPx.toDp() })
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { 
                        if (initialUri == null) {
                            onPickImage()
                        } else {
                            viewModel.toggleContrast()
                        }
                    }
                    .padding(if (initialUri == null) 32.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                if (initialUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Icon(
                             painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_add_photo_alternate_24), 
                             contentDescription = null, 
                             modifier = Modifier.size(64.dp),
                             tint = MaterialTheme.colorScheme.primary
                         )
                         Spacer(Modifier.size(8.dp))
                         Text(
                             stringResource(R.string.watermark_pick_image),
                             style = MaterialTheme.typography.bodyLarge,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                    }
                } else {

                    
                    // Implementing the "Last Success Persist" logic here locally
                    val current = previewState
                    var lastSuccess by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<WatermarkUiState.Success?>(null) }
                    
                    if (current is WatermarkUiState.Success) {
                        lastSuccess = current
                    }
                    
                    val showBlur = current is WatermarkUiState.Processing
                    
                    val blurRadius by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (showBlur) 16.dp else 0.dp,
                        label = "blur"
                    )
                    
                    val alpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showBlur) 0.6f else 1f,
                        label = "alpha"
                    )
                    
                    Box(contentAlignment = Alignment.Center) {
                        // Underlying Image
                        if (lastSuccess != null) {
                            Box(
                                modifier = Modifier
                                    .blur(blurRadius)
                                    .alpha(alpha)
                            ) {
                                WatermarkPreview(uiState = lastSuccess!!)
                            }
                        } else {
                            // First load?
                            if (current is WatermarkUiState.Processing) {
                                // First load, show nothing or placeholder maybe
                            } else {
                                WatermarkPreview(uiState = current)
                            }
                        }

                        // Overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBlur,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut()
                        ) {
                            LoadingIndicator()
                        }
                    }
                }
            }
            
            // Allow this part to take remaining space and scroll
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Controls Area
                    RoundedCardContainer(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Style Picker
                        SegmentedPicker(
                            items = WatermarkStyle.entries,
                            selectedItem = options.style,
                            onItemSelected = { viewModel.setStyle(it) },
                            labelProvider = { style ->
                                when (style) {
                                    WatermarkStyle.OVERLAY -> context.getString(R.string.watermark_style_overlay)
                                    WatermarkStyle.FRAME -> context.getString(R.string.watermark_style_frame)
                                }
                            },
                            iconProvider = { style ->
                                val iconRes = when (style) {
                                    WatermarkStyle.OVERLAY -> R.drawable.rounded_magnify_fullscreen_24
                                    WatermarkStyle.FRAME -> R.drawable.rounded_window_open_24
                                }
        
                                Icon(
                                     painter = androidx.compose.ui.res.painterResource(id = iconRes),
                                     contentDescription = null,
                                     modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
        
                        // Show Brand Toggle
                        IconToggleItem(
                            iconRes = R.drawable.rounded_info_24, 
                            title = stringResource(R.string.watermark_show_brand),
                            isChecked = options.showDeviceBrand,
                            onCheckedChange = { viewModel.setShowBrand(it) }
                        )
        
                        // Show EXIF Toggle
                        IconToggleItem(
                            iconRes = R.drawable.rounded_info_24, 
                            title = stringResource(R.string.watermark_show_exif),
                            isChecked = options.showExif,
                            onCheckedChange = { viewModel.setShowExif(it) }
                        )
                    }
                    
                    // Bottom spacing for scrolling
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
