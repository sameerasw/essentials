/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: UnsupportedFeaturesConfirmationSheet.kt
 * Description: Reusable core UI component for UnsupportedFeaturesConfirmationSheet.kt.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.ui.features.consciousgate.HoldToContinueButton
import com.sameerasw.essentials.ui.features.consciousgate.UnscaledMotion
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer

@Composable
fun UnsupportedFeaturesConfirmationSheet(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    featureTitleResIds: List<Int>,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val waitProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        withContext(UnscaledMotion) {
            waitProgress.animateTo(1f, tween(10_000, easing = LinearEasing))
        }
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TranslatableText(
                stringResId = R.string.unsupported_sheet_title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TranslatableText(
                stringResId = R.string.unsupported_sheet_body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    (capabilities + if (DeviceUtils.isTorchRestrictedDevice()) torchCapabilities else emptyList()).forEach { (titleRes, descriptionRes) ->
                        Column {
                            TranslatableText(
                                stringResId = titleRes,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            TranslatableText(
                                stringResId = descriptionRes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    featureTitleResIds.forEach { titleRes ->
                        TranslatableText(
                            stringResId = titleRes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            HoldToContinueButton(
                waitTimeProgress = waitProgress.value,
                onContinue = onConfirm,
                onHoldingChange = {},
                modifier = Modifier.fillMaxWidth(),
                holdDurationMillis = 3_000L,
                waitLabel = stringResource(R.string.unsupported_please_wait),
                holdLabel = stringResource(R.string.unsupported_hold_to_enable),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                progressColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
            OutlinedButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TranslatableText(R.string.action_cancel)
            }
        }
    }
}

private val capabilities =
    listOf(
        R.string.unsupported_cap_hidden_title to R.string.unsupported_cap_hidden_desc,
        R.string.unsupported_cap_tiles_title to R.string.unsupported_cap_tiles_desc,
        R.string.unsupported_cap_search_title to R.string.unsupported_cap_search_desc,
        R.string.unsupported_cap_freeze_title to R.string.unsupported_cap_freeze_desc,
        R.string.unsupported_cap_duo_title to R.string.unsupported_cap_duo_desc,
    )

private val torchCapabilities =
    listOf(
        R.string.unsupported_cap_flashlight_title to R.string.unsupported_cap_flashlight_desc,
    )
