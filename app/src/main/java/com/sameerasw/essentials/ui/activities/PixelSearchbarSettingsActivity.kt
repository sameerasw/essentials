package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.services.widgets.WidgetScraperService
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import androidx.compose.material3.Switch

import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sheets.FeatureHelpBottomSheet
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import android.content.Context
import android.content.Intent

class PixelSearchbarSettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = LocalContext.current

            remember(context) { viewModel.check(context) }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            var showHelpSheet by remember { mutableStateOf(false) }

            val pixelSearchbarFeature = remember {
                object : Feature(
                    id = "Pixel Searchbar",
                    title = R.string.pixel_searchbar_settings_title,
                    iconRes = R.drawable.rounded_search_24,
                    category = R.string.cat_display,
                    description = R.string.feat_pixel_searchbar_desc,
                    aboutDescription = R.string.about_desc_pixel_searchbar,
                    permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
                    showToggle = true,
                    hasMoreSettings = true
                ) {
                    override fun isEnabled(viewModel: MainViewModel) = viewModel.isPixelSearchbarEnabled.value
                    override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {
                        viewModel.setPixelSearchbarEnabled(enabled, context)
                    }
                }
            }

            val isBlurEnabled by viewModel.isBlurEnabled

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) { _ ->
                    val density = LocalDensity.current
                    val statusBarHeightPx = with(density) {
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .progressiveBlur(
                                blurRadius = if (isBlurEnabled) 40f else 0f,
                                height = statusBarHeightPx * 1.15f,
                                direction = BlurDirection.TOP
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = with(density) { 150.dp.toPx() },
                                    direction = BlurDirection.BOTTOM
                                )
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(
                                modifier = Modifier.height(
                                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                                )
                            )

                            PixelSearchbarSettingsUI(
                                viewModel = viewModel,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 150.dp
                                )
                            )
                        }

                        EssentialsFloatingToolbar(
                            title = stringResource(R.string.pixel_searchbar_settings_title),
                            onBackClick = { finish() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f),
                            onHelpClick = {
                                showHelpSheet = true
                            }
                        )

                        if (showHelpSheet) {
                            FeatureHelpBottomSheet(
                                onDismissRequest = { showHelpSheet = false },
                                feature = pixelSearchbarFeature,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixelSearchbarSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isEnabled = viewModel.isPixelSearchbarEnabled.value
    var showPermissionSheet by remember { mutableStateOf(false) }
    val currentType = viewModel.pixelSearchbarType.value

    val options = listOf("empty", "date", "widget", "music")
    val labels = mapOf(
        "empty" to stringResource(R.string.pixel_searchbar_style_empty),
        "date" to stringResource(R.string.pixel_searchbar_style_date),
        "widget" to stringResource(R.string.pixel_searchbar_style_widget),
        "music" to stringResource(R.string.pixel_searchbar_style_music)
    )

    val awm = remember { AppWidgetManager.getInstance(context) }
    val widgetHost = remember { AppWidgetHost(context, WidgetScraperService.HOST_ID) }

    // Track the allocated ID so we can deallocate on cancel
    var pendingWidgetId by remember { mutableStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }

    // Single launcher — ACTION_APPWIDGET_PICK handles bind permission internally
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val widgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val info = awm.getAppWidgetInfo(widgetId)
                val providerName = info?.provider?.flattenToString()
                viewModel.setPixelSearchbarType("widget", context)
                viewModel.setPixelSearchbarWidgetId(widgetId, providerName, context)
                WidgetScraperService.start(context)
            }
        } else {
            // Deallocate the ID we pre-allocated if user cancelled
            if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetHost.deleteAppWidgetId(pendingWidgetId)
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            }
        }
    }

    fun openWidgetPicker() {
        val allocatedId = widgetHost.allocateAppWidgetId()
        pendingWidgetId = allocatedId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
        }
        pickerLauncher.launch(pickIntent)
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_search_24,
                title = stringResource(R.string.feat_pixel_searchbar_title),
                description = "Replace Pixel Launcher default searchbar",
                isChecked = isEnabled,
                onCheckedChange = { enabled ->
                    if (viewModel.isWriteSecureSettingsEnabled.value || viewModel.isShizukuPermissionGranted.value || viewModel.isRootPermissionGranted.value) {
                        viewModel.setPixelSearchbarEnabled(enabled, context)
                    } else {
                        showPermissionSheet = true
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = isEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_replace_with),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoundedCardContainer {
                    SegmentedPicker(
                        items = options,
                        selectedItem = currentType,
                        onItemSelected = { type ->
                            HapticUtil.performVirtualKeyHaptic(view)
                            when {
                                type == "widget" -> openWidgetPicker()
                                type == "music" -> {
                                    WidgetScraperService.start(context)
                                    viewModel.setPixelSearchbarType(type, context)
                                }
                                currentType == "widget" || currentType == "music" -> {
                                    WidgetScraperService.stop(context)
                                    viewModel.setPixelSearchbarType(type, context)
                                }
                                else -> viewModel.setPixelSearchbarType(type, context)
                            }
                        },
                        labelProvider = { labels[it] ?: it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Widget mode controls
                AnimatedVisibility(
                    visible = currentType == "widget",
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val widgetProvider = viewModel.pixelSearchbarWidgetProvider.value
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RoundedCardContainer(spacing = 2.dp) {
                            ListItem(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    openWidgetPicker()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_widgets_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = if (widgetProvider != null)
                                            widgetProvider.substringAfterLast("/")
                                        else
                                            stringResource(R.string.pixel_searchbar_widget_none),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright
                                )
                            ) {
                                Text(
                                    text = if (widgetProvider != null)
                                        stringResource(R.string.pixel_searchbar_widget_change)
                                    else
                                        stringResource(R.string.pixel_searchbar_widget_picker_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            AnimatedVisibility(
                                visible = widgetProvider != null,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                ListItem(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        viewModel.clearPixelSearchbarWidget(context)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_delete_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.pixel_searchbar_widget_remove),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Horizontal and Vertical Padding sliders
                        RoundedCardContainer(spacing = 2.dp) {
                            ConfigSliderItem(
                                title = stringResource(R.string.pixel_searchbar_widget_padding_h),
                                value = viewModel.pixelSearchbarWidgetPaddingH.intValue.toFloat(),
                                onValueChange = {
                                    viewModel.pixelSearchbarWidgetPaddingH.intValue = it.toInt()
                                },
                                onValueChangeFinished = {
                                    viewModel.setPixelSearchbarWidgetPaddingH(
                                        viewModel.pixelSearchbarWidgetPaddingH.intValue,
                                        context
                                    )
                                },
                                valueRange = 0f..100f,
                                increment = 4f,
                                iconRes = R.drawable.rounded_rounded_corner_24
                            )
                            ConfigSliderItem(
                                title = stringResource(R.string.pixel_searchbar_widget_padding_v),
                                value = viewModel.pixelSearchbarWidgetPaddingV.intValue.toFloat(),
                                onValueChange = {
                                    viewModel.pixelSearchbarWidgetPaddingV.intValue = it.toInt()
                                },
                                onValueChangeFinished = {
                                    viewModel.setPixelSearchbarWidgetPaddingV(
                                        viewModel.pixelSearchbarWidgetPaddingV.intValue,
                                        context
                                    )
                                },
                                valueRange = 0f..100f,
                                increment = 4f,
                                iconRes = R.drawable.rounded_rounded_corner_24
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = currentType == "date",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentDateFormat = viewModel.pixelSearchbarDateFormat.value
                    val dateFormats = listOf(
                        "EEEE, MMMM d",
                        "EEEE, MMM d",
                        "EEE, MMM d",
                        "EEEE, d MMMM",
                        "d MMMM",
                        "MMMM d",
                        "EEE, d MMM",
                        "yyyy-MM-dd",
                        "dd/MM/yyyy"
                    )
                    val currentDate = remember { java.util.Date() }
                    val googleSansFlexRound = remember { FontFamily(Font(R.font.google_sans_flex_round)) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        RoundedCardContainer {
                            IconToggleItem(
                                iconRes = R.drawable.rounded_rounded_corner_24,
                                title = stringResource(R.string.pixel_searchbar_background_pill_title),
                                description = stringResource(R.string.pixel_searchbar_background_pill_desc),
                                isChecked = viewModel.pixelSearchbarBackgroundPill.value,
                                onCheckedChange = { enabled ->
                                    viewModel.setPixelSearchbarBackgroundPill(enabled, context)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Date Format",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        RoundedCardContainer(spacing = 2.dp) {
                            dateFormats.forEach { format ->
                                val isSelected = currentDateFormat == format
                                val formattedDate = remember(format, currentDate) {
                                    try {
                                        java.text.SimpleDateFormat(format, java.util.Locale.getDefault()).format(currentDate)
                                    } catch (e: Exception) {
                                        format
                                    }
                                }

                                ListItem(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        viewModel.setPixelSearchbarDateFormat(format, context)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingContent = {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                viewModel.setPixelSearchbarDateFormat(format, context)
                                            }
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    )
                                ) {
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = googleSansFlexRound
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap Action",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoundedCardContainer {
                    val tapActionEnabled = viewModel.pixelSearchbarTapActionEnabled.value
                    ListItem(
                        onClick = {
                            if (tapActionEnabled) {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val intent = Intent(context, com.sameerasw.essentials.MainActivity::class.java).apply {
                                    putExtra("target_tab", com.sameerasw.essentials.domain.DIYTabs.DIY.name)
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_rocket_launch_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (tapActionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = tapActionEnabled,
                                onCheckedChange = { enabled ->
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.setPixelSearchbarTapActionEnabled(enabled, context)
                                }
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.pixel_searchbar_tap_action_enabled_desc),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (tapActionEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.pixel_searchbar_tap_action_enabled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (tapActionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }

    if (showPermissionSheet) {
        val permissionItem = remember(context, viewModel) {
            com.sameerasw.essentials.utils.PermissionUIHelper.getPermissionItem("WRITE_SECURE_SETTINGS", context, viewModel)
        }
        if (permissionItem != null) {
            PermissionsBottomSheet(
                onDismissRequest = { showPermissionSheet = false },
                featureTitle = "Pixel Searchbar",
                permissions = listOf(permissionItem)
            )
        }
    }
}
