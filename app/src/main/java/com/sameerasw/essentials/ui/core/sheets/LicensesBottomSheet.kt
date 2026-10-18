/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: LicensesBottomSheet.kt
 * Description: Bottom sheet displaying open source licenses and credits, replicating Help & Guides sheet layout.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

data class LicenseSection(
    val title: String,
    val iconRes: Int = R.drawable.rounded_code_24,
    val description: String? = null,
    val licenseType: String,
    val licenseColor: Color = Color.Unspecified,
    val links: List<Pair<String, String>> = emptyList(), // Pair(label, url)
)

private fun getLicenseColor(licenseName: String): Color {
    val clean = licenseName.lowercase()
    return when {
        clean.contains("apache") -> Color(0xFF4CAF50)
        clean.contains("mit") -> Color(0xFF2196F3)
        clean.contains("bsd") -> Color(0xFFFF9800)
        clean.contains("gpl") -> Color(0xFFE91E63)
        clean.contains("mpl") || clean.contains("mozilla") -> Color(0xFF9C27B0)
        clean.contains("eclipse") || clean.contains("epl") -> Color(0xFF009688)
        clean.contains("creative commons") || clean.contains("cc-") -> Color(0xFF8D6E63)
        clean.contains("isc") -> Color(0xFF00ACC1)
        clean.contains("unlicense") -> Color(0xFF78909C)
        else -> ColorUtil.getVibrantColorFor(licenseName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesBottomSheet(onDismissRequest: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    val libraries by produceLibraries {
        context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
    }

    val customSections = remember {
        listOf(
            LicenseSection(
                title = "emojis.json",
                iconRes = R.drawable.rounded_emoji_language_24,
                description = "Comprehensive emoji dataset and categorization used in the built-in keyboard.",
                licenseType = "MIT License",
                licenseColor = getLicenseColor("MIT"),
                links = listOf(context.getString(R.string.action_view_on_github) to "https://github.com/chalda-pnuzig/emojis.json"),
            ),
            LicenseSection(
                title = "Watch eSIM Helper",
                iconRes = R.drawable.rounded_sim_card_24,
                description = "Pixel Watch eSIM handoff and activation code parsing used for the watch eSIM installer.",
                licenseType = "Apache License 2.0",
                licenseColor = getLicenseColor("Apache"),
                links = listOf(context.getString(R.string.action_view_on_github) to "https://github.com/AllMySlop/pixel-watch-esim-writer"),
            ),
            LicenseSection(
                title = "generate-kaomoji",
                iconRes = R.drawable.rounded_heart_smile_24,
                description = "Kaomoji dataset and unicode character combinations for keyboard kaomojis.",
                licenseType = "MIT License",
                licenseColor = getLicenseColor("MIT"),
                links = listOf(context.getString(R.string.action_view_on_github) to "https://github.com/xav-ie/generate-kaomoji"),
            ),
        )
    }

    val allSections = remember(libraries) {
        val libList = libraries?.libraries?.map { lib ->
            val license = lib.licenses.firstOrNull()
            val licenseName = license?.name ?: "Open Source"

            val links = mutableListOf<Pair<String, String>>()
            license?.url?.takeIf { it.isNotBlank() }?.let { links.add(context.getString(R.string.action_license) to it) }
            lib.website?.takeIf { it.isNotBlank() }?.let { links.add(context.getString(R.string.action_website) to it) }
            lib.scm?.url?.takeIf { it.isNotBlank() && it != lib.website }?.let { links.add(context.getString(R.string.action_source_code) to it) }

            LicenseSection(
                title = lib.name.ifBlank { lib.uniqueId },
                iconRes = R.drawable.rounded_code_24,
                description = lib.description?.takeIf { it.isNotBlank() },
                licenseType = licenseName + (lib.artifactVersion?.let { " • v$it" } ?: ""),
                licenseColor = getLicenseColor(licenseName),
                links = links,
            )
        } ?: emptyList()

        customSections + libList
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.licenses_credits_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.licenses_credits_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            item {
                RoundedCardContainer {
                    allSections.forEach { section ->
                        ExpandableLicenseSection(section)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableLicenseSection(section: LicenseSection) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrow_rotation",
    )
    val context = LocalContext.current
    val view = LocalView.current

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    expanded = !expanded
                },
        shape = RoundedCornerShape(4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = if (expanded) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = section.iconRes),
                            contentDescription = null,
                            tint = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = section.licenseType,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (section.licenseColor != Color.Unspecified) section.licenseColor else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                    contentDescription =
                        if (expanded) {
                            stringResource(R.string.action_collapse)
                        } else {
                            stringResource(R.string.action_expand)
                        },
                    modifier = Modifier.rotate(rotation),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier =
                        Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (section.description != null) {
                        Text(
                            text = section.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f),
                        )
                    }

                    if (section.links.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            section.links.forEach { (label, url) ->
                                val iconRes = when {
                                    url.contains("github.com", ignoreCase = true) -> R.drawable.brand_github
                                    label == stringResource(R.string.action_license) -> R.drawable.rounded_description_24
                                    label == stringResource(R.string.action_website) -> R.drawable.rounded_web_24
                                    else -> R.drawable.rounded_code_24
                                }
                                OutlinedButton(
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
