/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Apps
 * File: StandbyAppsMoveSheet.kt
 * Description: UI bottom sheet for selecting a target App Standby Bucket to move selected apps.
 */

package com.sameerasw.essentials.ui.features.apps.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun StandbyAppsMoveSheet(
    onDismissRequest: () -> Unit,
    onBucketSelected: (Int) -> Unit,
) {
    val view = LocalView.current

    val buckets =
        listOf(
            10 to R.string.standby_bucket_active,
            20 to R.string.standby_bucket_working_set,
            30 to R.string.standby_bucket_frequent,
            40 to R.string.standby_bucket_rare,
            45 to R.string.standby_bucket_restricted,
        )

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TranslatableText(
                    stringResId = R.string.standby_apps_move_sheet_title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(buckets) { _, (bucketCode, titleRes) ->
                        ListItem(
                            checked = false,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onBucketSelected(bucketCode)
                            },
                            onLongClick = null,
                            enabled = true,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            leadingContent = null,
                            supportingContent = null,
                            trailingContent = {
                                RadioButton(
                                    selected = false,
                                    onClick = null,
                                )
                            },
                            colors =
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                ),
                            contentPadding =
                                PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 16.dp,
                                ),
                            content = {
                                TranslatableText(
                                    stringResId = titleRes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
