/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: RepoDetailsRow.kt
 * Description: UI row displaying repository info and stats from sameerasw.com.
 */

package com.sameerasw.essentials.ui.components.dialogs

import android.content.Intent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.ProjectDetailsRepository
import com.sameerasw.essentials.domain.model.github.ProjectDetails
import com.sameerasw.essentials.ui.theme.AppFontFamily
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun RepoDetailsRow(
    modifier: Modifier = Modifier,
    repoUrl: String = "https://github.com/sameerasw/essentials",
) {
    val context = LocalContext.current
    val view = LocalView.current
    var projectDetails by remember { mutableStateOf<ProjectDetails?>(null) }

    LaunchedEffect(Unit) {
        val repo = ProjectDetailsRepository.getInstance(context)
        projectDetails = repo.getProjectDetails("essentials")
    }

    val repoName = projectDetails?.repo ?: "sameerasw/essentials"

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    val intent = Intent(Intent.ACTION_VIEW, repoUrl.toUri())
                    context.startActivity(intent)
                },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.brand_github),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = repoName,
                    style =
                        MaterialTheme.typography.labelLarge.copy(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val stars = projectDetails?.stars ?: 0
                if (stars > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceBright,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.round_star_24),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (stars >= 1000) String.format("%.1fk", stars / 1000f) else "$stars",
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                val downloads = projectDetails?.downloads ?: 0
                if (downloads > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceBright,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_download_24),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (downloads >= 1000) "${downloads / 1000}k" else "$downloads",
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
