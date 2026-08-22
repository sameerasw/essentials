/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: FloatingWithFragment.kt
 * Description: UI layout for the Tools tab built entirely with Essentials core design components (RoundedCardContainer, EssentialsBottomSheet).
 */

package com.sameerasw.essentials.ui.components.linkActions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.QrCodeGenerator
import com.sameerasw.essentials.utils.WindowingUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingWithContent(
    uri: Uri,
    onSelectTab: (Int) -> Unit = {},
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showQrSheet by remember { mutableStateOf(false) }
    val isFloatingSupported = remember { WindowingUtils.isFloatingModeSupported(context) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 1. Preview Web (Only shown when floating mode is supported)
            if (isFloatingSupported) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                WindowingUtils.launchOverlayWindow(context, uri, isPrivate = true)
                                onFinish()
                            }
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                            )
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_open_in_browser_24),
                            contentDescription = "Preview Web",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.preview_web_title),
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.preview_web_desc),
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }

            // 2. Instant QR Code Generator
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            showQrSheet = true
                        }
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                        )
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_devices_24),
                        contentDescription = "QR Code",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.qr_code_title),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.qr_code_desc),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    // Replaced ModalBottomSheet with EssentialsBottomSheet core component
    if (showQrSheet) {
        val appLogo = remember { QrCodeGenerator.getAppLogoBitmap(context) }
        val qrBitmap = remember(uri, appLogo) {
            QrCodeGenerator.generateQrBitmap(
                content = uri.toString(),
                size = 600,
                foregroundColor = android.graphics.Color.BLACK,
                backgroundColor = android.graphics.Color.WHITE,
                logo = appLogo,
            )
        }

        EssentialsBottomSheet(
            onDismissRequest = { showQrSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.qr_code_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = uri.host ?: uri.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                // High-Contrast Rounded QR Code Display with Center App Logo
                Box(
                    modifier =
                        Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code with Logo for $uri",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons Row: Copy Link & Share QR Code (Image + Text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Link", uri.toString()))
                            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_content_copy_24),
                            contentDescription = "Copy",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Copy Link")
                    }

                    ElevatedButton(
                        onClick = {
                            val qrUri = QrCodeGenerator.getShareableQrUri(context, qrBitmap)
                            val shareMessage = "Check out this link: $uri"

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                if (qrUri != null) {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, qrUri)
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                }
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code & Link"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_share_24),
                            contentDescription = "Share",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Share QR")
                    }
                }

                // Switch to Share Tab Button
                FilledTonalButton(
                    onClick = {
                        showQrSheet = false
                        onSelectTab(1)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_open_in_new_24),
                        contentDescription = "Share Tab",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Open in Share Tab")
                }
            }
        }
    }
}
