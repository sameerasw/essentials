/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: AboutSection.kt
 * Description: UI layout element for AboutSection.kt.
 */

package com.sameerasw.essentials.ui.components.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.sheets.LicensesBottomSheet
import com.sameerasw.essentials.utils.HapticUtil

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AboutSection(
    modifier: Modifier = Modifier,
    appName: String = "Essentials",
    developerName: String = stringResource(R.string.app_developer_name),
    description: String = stringResource(R.string.app_description),
    onAvatarLongClick: () -> Unit = {},
    onAvatarLongClickWithPosition: ((Offset) -> Unit)? = null,
) {
    val view = LocalView.current
    var showLicensesSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val versionName =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            "Unknown"
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "$appName v$versionName", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            var avatarCenterOffset by remember { mutableStateOf(Offset.Zero) }

            Image(
                painter = painterResource(id = R.drawable.avatar),
                contentDescription = "Developer Avatar",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(120.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            val size = coords.size
                            avatarCenterOffset = Offset(
                                x = pos.x + (size.width / 2f),
                                y = pos.y + (size.height / 2f)
                            )
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                onAvatarLongClick()
                                onAvatarLongClickWithPosition?.invoke(avatarCenterOffset)
                            },
                        ),
            )

            Text(
                text = stringResource(R.string.developed_by_format, developerName),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 3,
            ) {
                Button(
                    onClick = {
                        val websiteUrl = "https://sameerasw.com"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_web_traffic_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_website))
                }

                Button(
                    onClick = {
                        val websiteUrl = "https://github.com/sameerasw/essentials"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.brand_github),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_view_on_github))
                }

                OutlinedButton(
                    onClick = {
                        // Use mailto: URI so the system opens an email client
                        val mailUri = "mailto:mail@sameerasw.com".toUri()
                        val emailIntent =
                            Intent(Intent.ACTION_SENDTO, mailUri).apply {
                                putExtra(Intent.EXTRA_SUBJECT, "Hello from Essentials")
                            }
                        try {
                            context.startActivity(
                                Intent.createChooser(
                                    emailIntent,
                                    context.getString(R.string.send_email_chooser_title),
                                ),
                            )
                        } catch (e: ActivityNotFoundException) {
                            Log.w("AboutSection", "No email app available", e)
                            Toast
                                .makeText(context, R.string.error_no_email_app, Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_mail_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_contact))
                }

                OutlinedButton(
                    onClick = {
                        val websiteUrl = "https://t.me/tidwib"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.brand_telegram),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_telegram))
                }

                OutlinedButton(
                    onClick = {
                        val websiteUrl = "https://buymeacoffee.com/sameerasw"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_heart_smile_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_support))
                }
            }

            Text(
                text = stringResource(R.string.label_other_apps),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 3,
            ) {
                OutlinedButton(
                    onClick = {
                        val websiteUrl =
                            "https://play.google.com/store/apps/details?id=com.sameerasw.airsync&hl=en"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_devices_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_airsync))
                }

                OutlinedButton(
                    onClick = {
                        val websiteUrl = "https://sameerasw.com/zen"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_web_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_zenzero))
                }

                OutlinedButton(
                    onClick = {
                        val websiteUrl = "https://github.com/sameerasw/canvas"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_draw_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_canvas))
                }

                OutlinedButton(
                    onClick = {
                        val websiteUrl = "https://github.com/sameerasw/tasks"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_task_alt_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_tasks))
                }

                OutlinedButton(
                    onClick = {
                        val websiteUrl = "https://github.com/sameerasw/Browser"
                        val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_highlight_mouse_cursor_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_zero))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    showLicensesSheet = true
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_code_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_licenses_and_credits))
            }
        }
    }

    if (showLicensesSheet) {
        LicensesBottomSheet(
            onDismissRequest = { showLicensesSheet = false },
        )
    }
}
