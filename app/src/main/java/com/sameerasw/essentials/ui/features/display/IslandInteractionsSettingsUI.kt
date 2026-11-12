package com.sameerasw.essentials.ui.features.display

import android.content.Intent
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.IslandPriorityEntries
import com.sameerasw.essentials.island.remote.ProviderDiscovery
import com.sameerasw.essentials.island.remote.ProviderPriorities
import com.sameerasw.essentials.island.remote.ProviderTrustStore
import com.sameerasw.essentials.island.ui.IslandBackdropBlurSupport
import com.sameerasw.essentials.services.handlers.SoundModeHandler
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.pickers.ISLAND_ICON_PASTEL_COLORS
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.BackdropBlurUnavailableBottomSheet
import com.sameerasw.essentials.ui.core.sheets.HideStatusBarInfoBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PreferStillIslandInfoBottomSheet
import com.sameerasw.essentials.ui.core.sheets.StatusBarAwareWidthInfoBottomSheet
import com.sameerasw.essentials.ui.features.consciousgate.CONSCIOUS_GATE_FEATURE_ID
import com.sameerasw.essentials.ui.features.display.actions.GestureActionPickerSheet
import com.sameerasw.essentials.ui.features.display.actions.HorizontalSlideModeSheet
import com.sameerasw.essentials.ui.features.display.actions.horizontalSlideDescription
import com.sameerasw.essentials.ui.features.display.sheets.IslandAlarmOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandAppOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBatteryOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBorderOutlineOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBriefOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandCaffeinateOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandCallOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandDevicesBatteryBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandLiveUpdatesOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandNavigationOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandNotificationHubOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandPrefToggle
import com.sameerasw.essentials.ui.features.display.sheets.IslandPulseShadowOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandSignalOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandSoundModeOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimeOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimerOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceCalendarOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.rememberIslandShowsWhileLocked
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlin.math.ceil
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableColumn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandInteractionsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val previewSettings = remember { SettingsRepository(context) }
    var dismissDirection by remember { mutableStateOf(previewSettings.getIslandDismissDirection()) }
    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    var pickingGesture by remember { mutableStateOf<String?>(null) }
    var showSlideModeSheet by remember { mutableStateOf(false) }
    val soundModeHandler = remember { SoundModeHandler(context) }
    val essentialsPrefs = remember { context.getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE) }
    var soundModeUseShizuku by remember { mutableStateOf(essentialsPrefs.getBoolean("sound_mode_use_shizuku", true)) }

    requestingPermissionsFor?.let { (titleRes, permKeys) ->
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = stringResource(titleRes),
            permissions = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TranslatableText(
            stringResId = R.string.duo_section_actions,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                    .padding(top = 12.dp)
                    .highlight(highlightSetting == "island_dismiss_direction"),
            ) {
                TranslatableText(
                    stringResId = R.string.island_dismiss_direction_title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val horizontalLabel = stringResource(R.string.island_dismiss_direction_horizontal)
                val downLabel = stringResource(R.string.island_dismiss_direction_down)
                SegmentedPicker(
                    items = listOf(SettingsRepository.ISLAND_DISMISS_HORIZONTAL, SettingsRepository.ISLAND_DISMISS_DOWN),
                    selectedItem = dismissDirection,
                    onItemSelected = {
                        dismissDirection = it
                        previewSettings.setIslandDismissDirection(it)
                    },
                    labelProvider = { if (it == SettingsRepository.ISLAND_DISMISS_DOWN) downLabel else horizontalLabel },
                    title = R.string.island_dismiss_direction_title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IconToggleItem(
                iconRes = R.drawable.rounded_open_in_new_24,
                title = stringResource(R.string.island_tap_to_open_expanded_title),
                isChecked = viewModel.isIslandTapToOpenExpanded.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandTapToOpenExpanded(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_tap_to_open_expanded"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.island_dismiss_on_outside_title),
                isChecked = viewModel.isIslandDismissOnOutside.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandDismissOnOutside(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_dismiss_on_outside"),
            )
        }

        TranslatableText(
            stringResId = R.string.island_section_gestures,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_front_hand_24,
                title = stringResource(R.string.duo_action_long_press_title),
                description = if (viewModel.isIslandBriefEnabled.value) {
                    stringResource(R.string.island_brief_title)
                } else {
                    viewModel.islandLongPressAction.value?.let { stringResource(it.title) }
                        ?: stringResource(R.string.duo_action_none)
                },
                showToggle = false,
                enabled = !viewModel.isIslandBriefEnabled.value,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    pickingGesture = "long_press"
                },
                modifier = Modifier.highlight(highlightSetting == "island_long_press_action"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_favorite_24,
                title = stringResource(R.string.island_like_while_playing_title),
                isChecked = viewModel.isIslandLikeWhilePlaying.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandLikeWhilePlayingEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_like_while_playing"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_compare_arrows_24,
                title = stringResource(R.string.duo_action_horizontal_slide_title),
                description = horizontalSlideDescription(viewModel.islandSlideMode.value, viewModel.isIslandSlideTrack.value),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showSlideModeSheet = true
                },
                modifier = Modifier.highlight(highlightSetting == "island_slide_mode"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_skip_next_24,
                title = stringResource(R.string.duo_action_horizontal_slide_track),
                description = stringResource(R.string.duo_action_horizontal_slide_track_desc),
                isChecked = viewModel.isIslandSlideTrack.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandSlideTrackEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_slide_track"),
            )

            AnimatedVisibility(
                visible = viewModel.islandSlideMode.value == "sound_mode",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = stringResource(R.string.sound_mode_use_shizuku_title),
                    isChecked = soundModeUseShizuku,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        soundModeUseShizuku = checked
                        essentialsPrefs.edit().putBoolean("sound_mode_use_shizuku", checked).apply()
                        if (!soundModeHandler.canChangeMode() || (checked && !soundModeHandler.isShizukuReady())) {
                            viewModel.check(context)
                            requestingPermissionsFor =
                                R.string.duo_action_horizontal_slide_sound_mode to
                                if (checked) listOf("SHIZUKU") else listOf("NOTIFICATION_POLICY")
                        }
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_slide_sound_mode_shizuku"),
                )
            }

            AnimatedVisibility(
                visible = viewModel.isIslandSlideTrack.value || viewModel.islandSlideMode.value == "sound_mode",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_compare_arrows_24,
                    title = stringResource(R.string.duo_slide_mirror_direction_title),
                    isChecked = viewModel.isIslandSlideInvertDirection.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandSlideInvertDirection(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_slide_invert_direction"),
                )
            }
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.island_always_gestures_title),
                description = stringResource(R.string.island_always_gestures_desc),
                isChecked = viewModel.isIslandAlwaysGestures.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandAlwaysGestures(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_always_gestures"),
            )
        }

        AnimatedVisibility(
            visible = !viewModel.isIslandShowTimeBattery.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            TranslatableText(
                stringResId = R.string.island_gesture_visuals_hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (pickingGesture != null) {
        GestureActionPickerSheet(
            viewModel = viewModel,
            currentAction = viewModel.islandLongPressAction.value,
            onActionSelected = viewModel::setIslandLongPressAction,
            onPickerClosed = { pickingGesture = null },
        )
    }

    if (showSlideModeSheet) {
        HorizontalSlideModeSheet(
            mode = viewModel.islandSlideMode.value,
            onModeSelected = { mode ->
                viewModel.setIslandSlideMode(mode)
                if (mode == "sound_mode" && !soundModeHandler.canChangeMode()) {
                    viewModel.check(context)
                    requestingPermissionsFor = R.string.duo_action_horizontal_slide_sound_mode to listOf("SHIZUKU", "NOTIFICATION_POLICY")
                }
            },
            onDismissRequest = { showSlideModeSheet = false },
        )
    }
}
