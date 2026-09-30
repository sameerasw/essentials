/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: IslandWeatherOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring the Island weather plugin and its data source.
 */

package com.sameerasw.essentials.ui.features.weather

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.weather.WeatherRepository
import com.sameerasw.essentials.weather.effects.WeatherSimulation
import com.sameerasw.essentials.weather.location.DeviceLocationSource
import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.WeatherError
import com.sameerasw.essentials.weather.provider.OpenMeteoModels
import com.sameerasw.essentials.weather.provider.WeatherProviderException
import com.sameerasw.essentials.weather.provider.WeatherProviders
import kotlinx.coroutines.launch
import java.util.Date

import com.sameerasw.essentials.utils.WeatherLauncher

private val REFRESH_OPTIONS = listOf(30, 60, 120, 180, 360)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    var providerId by remember { mutableStateOf(WeatherProviders.resolve(settings.getWeatherProvider(), settings.getWeatherApiKey("weatherapi")).id) }
    val provider = WeatherProviders.byId(providerId)
    val weatherState by WeatherRepository.state.collectAsState()

    var experimental by remember { mutableStateOf(settings.isWeatherExperimentalEnabled()) }
    var simulatedWeather by remember { mutableStateOf(settings.getString(SettingsRepository.KEY_DEBUG_SIMULATED_WEATHER, WeatherSimulation.OFF) ?: WeatherSimulation.OFF) }
    var simulatedTime by remember { mutableStateOf(settings.getString(SettingsRepository.KEY_DEBUG_SIMULATED_TIME, "auto") ?: "auto") }
    var simulatedTemp by remember { mutableStateOf(settings.getString(SettingsRepository.KEY_DEBUG_SIMULATED_TEMP, "auto") ?: "auto") }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) experimental = settings.isWeatherExperimentalEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var showInLauncher by remember { mutableStateOf(WeatherLauncher.isVisible(context)) }
    var effects by remember { mutableStateOf(settings.isIslandWeatherEffectsEnabled()) }
    var weatherHaptics by remember { mutableStateOf(settings.isIslandWeatherHapticsEnabled()) }
    var units by remember { mutableStateOf(settings.getWeatherUnits()) }
    var refreshMinutes by remember { mutableIntStateOf(settings.getWeatherRefreshMinutes()) }
    var savedKey by remember { mutableStateOf(settings.getWeatherApiKey(providerId).orEmpty()) }
    var keyInput by remember { mutableStateOf(savedKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var locationMode by remember { mutableStateOf(settings.getWeatherLocationMode()) }
    var manualLabel by remember { mutableStateOf(settings.getWeatherManualLocation()?.third) }
    var cityQuery by remember { mutableStateOf("") }
    var cityResults by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<Int?>(null) }

    fun refreshNow() {
        scope.launch { WeatherRepository.refresh(context, force = true) }
    }

    val requestLocation = rememberLocationPermissionRequest { granted ->
        viewModel.check(context)
        if (granted) refreshNow()
    }



    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            SectionTitle(R.string.weather_section_source)
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigPickerItem(
                    title = stringResource(R.string.weather_provider_title),
                    selectedValue = provider.displayName,
                    iconRes = R.drawable.rounded_cloud_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WeatherProviders.all.forEach { option ->
                        SegmentedDropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                if (option.id != providerId) {
                                    settings.setWeatherProvider(option.id)
                                    providerId = option.id
                                    savedKey = settings.getWeatherApiKey(option.id).orEmpty()
                                    keyInput = savedKey
                                    scope.launch {
                                        WeatherRepository.clear(context)
                                        WeatherRepository.refresh(context, force = true)
                                    }
                                }
                            },
                        )
                    }
                }
                if (provider.id == "openmeteo") {
                    var modelId by remember { mutableStateOf(settings.getWeatherOpenMeteoModel()) }
                    ConfigPickerItem(
                        title = stringResource(R.string.weather_model_title),
                        selectedValue = OpenMeteoModels.label(modelId),
                        iconRes = R.drawable.rounded_cloud_24,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OpenMeteoModels.all.forEach { option ->
                            SegmentedDropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    if (option.id != modelId) {
                                        settings.setWeatherOpenMeteoModel(option.id)
                                        modelId = option.id
                                        scope.launch {
                                            WeatherRepository.clear(context)
                                            WeatherRepository.refresh(context, force = true)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                if (provider.requiresApiKey) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it.trim() },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(stringResource(R.string.weather_api_key_title)) },
                                leadingIcon = { Icon(painterResource(R.drawable.rounded_key_24), contentDescription = null) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            clipboardText(context)?.let { keyInput = it.trim() }
                                        }) {
                                            Icon(painterResource(R.drawable.rounded_content_paste_24), contentDescription = stringResource(R.string.shorten_paste_button))
                                        }
                                        IconButton(onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            keyVisible = !keyVisible
                                        }) {
                                            Icon(
                                                painterResource(if (keyVisible) R.drawable.rounded_visibility_off_24 else R.drawable.rounded_visibility_24),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                },
                                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    autoCorrectEnabled = false,
                                    imeAction = ImeAction.Done,
                                ),
                                shape = RoundedCornerShape(16.dp),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                provider.signupUrl?.let { url ->
                                    TextButton(onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        openUrl(context, url)
                                    }) {
                                        Text(stringResource(R.string.weather_get_api_key))
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        settings.setWeatherApiKey(providerId, keyInput)
                                        savedKey = keyInput
                                        refreshNow()
                                    },
                                    enabled = keyInput != savedKey,
                                ) {
                                    Text(stringResource(R.string.action_save))
                                }
                            }
                        }
                    }
                }
                val sourceError = weatherState.error?.takeIf { it !is WeatherError.LocationPermission && it !is WeatherError.NoLocation }
                AnimatedVisibility(visible = sourceError != null) {
                    val shownError = remember { mutableStateOf(sourceError) }
                    if (sourceError != null) shownError.value = sourceError
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.rounded_warning_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = shownError.value?.let { errorText(context, it) }.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            SectionTitle(R.string.perm_location_title)
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                SegmentedPicker(
                    items = listOf("device", "manual"),
                    selectedItem = locationMode,
                    onItemSelected = {
                        locationMode = it
                        settings.setWeatherLocationMode(it)
                        if (it == "device" && !DeviceLocationSource.hasPermission(context)) {
                            requestLocation()
                        } else {
                            refreshNow()
                        }
                    },
                    labelProvider = {
                        context.getString(if (it == "manual") R.string.weather_location_manual else R.string.weather_location_device)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (locationMode == "device" && !DeviceLocationSource.hasPermission(context)) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_my_location_24,
                        title = stringResource(R.string.weather_error_location_permission),
                        showToggle = false,
                        onClick = { requestLocation() },
                    )
                }
                if (locationMode == "manual") {
                    Surface(color = MaterialTheme.colorScheme.surfaceBright, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            manualLabel?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painterResource(R.drawable.rounded_location_on_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                            OutlinedTextField(
                                value = cityQuery,
                                onValueChange = { cityQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.weather_search_city)) },
                                leadingIcon = { Icon(painterResource(R.drawable.rounded_location_city_24), contentDescription = null) },
                                trailingIcon = {
                                    if (searching) {
                                        LoadingIndicator(Modifier.size(24.dp))
                                    } else {
                                        IconButton(
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                searching = true
                                                searchError = null
                                                scope.launch {
                                                    try {
                                                        cityResults = provider.searchCities(cityQuery, settings.getWeatherApiKey(providerId))
                                                        if (cityResults.isEmpty()) searchError = R.string.weather_search_no_results
                                                    } catch (e: WeatherProviderException) {
                                                        searchError = if (e.reason == WeatherProviderException.Reason.INVALID_KEY) {
                                                            R.string.weather_error_missing_key
                                                        } else {
                                                            R.string.weather_error_network
                                                        }
                                                    } finally {
                                                        searching = false
                                                    }
                                                }
                                            },
                                            enabled = cityQuery.isNotBlank(),
                                        ) {
                                            Icon(painterResource(R.drawable.rounded_search_24), contentDescription = stringResource(R.string.action_search))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                            )
                            searchError?.let {
                                Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            cityResults.forEach { city ->
                                Text(
                                    text = city.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            settings.setWeatherManualLocation(city.latitude, city.longitude, city.name)
                                            manualLabel = city.name
                                            cityResults = emptyList()
                                            cityQuery = ""
                                            refreshNow()
                                        }
                                        .padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }

            SectionTitle(R.string.weather_section_display)
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                SegmentedPicker(
                    items = listOf(
                        SettingsRepository.WEATHER_UNITS_SYSTEM,
                        SettingsRepository.WEATHER_UNITS_CELSIUS,
                        SettingsRepository.WEATHER_UNITS_FAHRENHEIT,
                    ),
                    selectedItem = units,
                    onItemSelected = {
                        units = it
                        settings.setWeatherUnits(it)
                    },
                    labelProvider = {
                        when (it) {
                            SettingsRepository.WEATHER_UNITS_CELSIUS -> "°C"
                            SettingsRepository.WEATHER_UNITS_FAHRENHEIT -> "°F"
                            else -> context.getString(R.string.weather_units_system)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    title = R.string.weather_units_title,
                )
                ConfigPickerItem(
                    title = stringResource(R.string.weather_refresh_interval_title),
                    selectedValue = intervalLabel(context, refreshMinutes),
                    iconRes = R.drawable.rounded_schedule_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    REFRESH_OPTIONS.forEach { minutes ->
                        SegmentedDropdownMenuItem(
                            text = { Text(intervalLabel(context, minutes)) },
                            onClick = {
                                refreshMinutes = minutes
                                settings.setWeatherRefreshMinutes(minutes)
                            },
                        )
                    }
                }
                IconToggleItem(
                    iconRes = R.drawable.rounded_rainy_24,
                    title = stringResource(R.string.island_weather_effects_title),
                    isChecked = effects,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        effects = it
                        settings.setIslandWeatherEffectsEnabled(it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_thunderstorm_24,
                    title = stringResource(R.string.island_weather_haptics_title),
                    isChecked = weatherHaptics,
                    enabled = effects,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        weatherHaptics = it
                        settings.setIslandWeatherHapticsEnabled(it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_partly_cloudy_day_24,
                    title = stringResource(R.string.weather_show_in_launcher),
                    isChecked = showInLauncher,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showInLauncher = it
                        WeatherLauncher.setVisible(context, it)
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = statusText(context, weatherState.snapshot?.updatedAt, weatherState.error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (weatherState.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (weatherState.loading) {
                    LoadingIndicator(Modifier.size(32.dp))
                } else {
                    OutlinedButton(onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        refreshNow()
                    }) {
                        Text(stringResource(R.string.action_refresh))
                    }
                }
            }

        if (experimental) {
            SectionTitle(R.string.weather_section_experimental)
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigPickerItem(
                    title = stringResource(R.string.dev_simulate_weather_title),
                    iconRes = R.drawable.rounded_partly_cloudy_day_24,
                    selectedValue = WeatherSimulation.presets.firstOrNull { it.id == simulatedWeather }?.label.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WeatherSimulation.presets.forEach { preset ->
                        SegmentedDropdownMenuItem(
                            text = { Text(preset.label) },
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                simulatedWeather = preset.id
                                settings.putString(SettingsRepository.KEY_DEBUG_SIMULATED_WEATHER, preset.id)
                            },
                        )
                    }
                }
                SegmentedPicker(
                    items = listOf("auto", "dawn", "day", "dusk", "night"),
                    selectedItem = simulatedTime,
                    onItemSelected = {
                        simulatedTime = it
                        settings.putString(SettingsRepository.KEY_DEBUG_SIMULATED_TIME, it)
                    },
                    labelProvider = {
                        context.getString(
                            when (it) {
                                "dawn" -> R.string.weather_sim_time_dawn
                                "day" -> R.string.weather_sim_time_day
                                "dusk" -> R.string.weather_sim_time_dusk
                                "night" -> R.string.weather_sim_time_night
                                else -> R.string.weather_sim_time_auto
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    title = R.string.weather_sim_time_title,
                )
                SegmentedPicker(
                    items = listOf("auto", "-15", "5", "22", "38", "50"),
                    selectedItem = simulatedTemp,
                    onItemSelected = {
                        simulatedTemp = it
                        settings.putString(SettingsRepository.KEY_DEBUG_SIMULATED_TEMP, it)
                    },
                    labelProvider = { if (it == "auto") context.getString(R.string.weather_sim_time_auto) else "$it°" },
                    modifier = Modifier.fillMaxWidth(),
                    title = R.string.weather_sim_temp_title,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    )
}

private fun intervalLabel(context: Context, minutes: Int): String =
    if (minutes < 60) {
        context.getString(R.string.weather_interval_minutes, minutes)
    } else {
        context.getString(R.string.weather_interval_hours, minutes / 60)
    }

private fun errorText(context: Context, error: WeatherError): String {
    val base = context.getString(
        when (error) {
            WeatherError.MissingApiKey -> R.string.weather_error_missing_key
            WeatherError.InvalidApiKey -> R.string.weather_error_invalid_key
            WeatherError.LocationPermission -> R.string.weather_error_location_permission
            WeatherError.NoLocation -> R.string.weather_error_no_location
            WeatherError.Network -> R.string.weather_error_network
            is WeatherError.Unknown -> R.string.weather_error_unknown
        },
    )
    val detail = (error as? WeatherError.Unknown)?.message?.takeIf { it.isNotBlank() }
    return if (detail != null) "$base: $detail" else base
}

private fun statusText(context: Context, updatedAt: Long?, error: WeatherError?): String = when {
    error != null -> errorText(context, error)
    updatedAt != null -> context.getString(
        R.string.weather_updated_at,
        DateFormat.getTimeFormat(context).format(Date(updatedAt)),
    )
    else -> context.getString(R.string.weather_not_loaded)
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}
