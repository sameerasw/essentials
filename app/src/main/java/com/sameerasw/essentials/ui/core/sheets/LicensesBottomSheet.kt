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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

data class LicenseSection(
    val title: String,
    val iconRes: Int = R.drawable.rounded_code_24,
    val description: String? = null,
    val licenseType: String,
    val licenseText: String,
    val links: List<Pair<String, String>> = emptyList(), // Pair(label, url)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesBottomSheet(onDismissRequest: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val sections = remember {
        listOf(
            LicenseSection(
                title = "emojis.json",
                iconRes = R.drawable.rounded_emoji_language_24,
                description = "Comprehensive emoji dataset and categorization used in the built-in keyboard.",
                licenseType = "MIT License",
                links = listOf("View on GitHub" to "https://github.com/chalda-pnuzig/emojis.json"),
                licenseText = """
                    MIT License

                    Copyright (c) 2020 chalda-pnuzig

                    Permission is hereby granted, free of charge, to any person obtaining a copy
                    of this software and associated documentation files (the "Software"), to deal
                    in the Software without restriction, including without limitation the rights
                    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                    copies of the Software, and to permit persons to whom the Software is
                    furnished to do so, subject to the following conditions:

                    The above copyright notice and this permission notice shall be included in all
                    copies or substantial portions of the Software.

                    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
                    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
                    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
                    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
                    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
                    SOFTWARE.
                """.trimIndent(),
            ),
            LicenseSection(
                title = "generate-kaomoji",
                iconRes = R.drawable.rounded_heart_smile_24,
                description = "Kaomoji dataset and unicode character combinations for keyboard kaomojis.",
                licenseType = "MIT License",
                links = listOf("View on GitHub" to "https://github.com/xav-ie/generate-kaomoji"),
                licenseText = """
                    MIT License

                    Copyright (c) 2021 Xavier

                    Permission is hereby granted, free of charge, to any person obtaining a copy
                    of this software and associated documentation files (the "Software"), to deal
                    in the Software without restriction, including without limitation the rights
                    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                    copies of the Software, and to permit persons to whom the Software is
                    furnished to do so, subject to the following conditions:

                    The above copyright notice and this permission notice shall be included in all
                    copies or substantial portions of the Software.

                    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
                    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
                    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
                    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
                    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
                    SOFTWARE.
                """.trimIndent(),
            ),
        )
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

            item {
                RoundedCardContainer {
                    sections.forEach { section ->
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
                        color = MaterialTheme.colorScheme.primary,
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

            // Content with AnimatedVisibility exactly like Help & Guides
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier =
                        Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth(fraction = 0.95f),
                    ) {
                        Text(
                            text = section.licenseText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }

                    if (section.links.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            section.links.forEach { (label, url) ->
                                OutlinedButton(
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.brand_github),
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
