/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: WearOS Companion
 * File: WatchEsimInstallSheet.kt
 * Description: Bottom sheet to scan or enter an eSIM activation code and continue in the Pixel Watch app.
 */

package com.sameerasw.essentials.ui.features.watch

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.WatchEsimHandoff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchEsimInstallSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var input by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    val companionAvailable = remember { WatchEsimHandoff.isCompanionAvailable(context) }

    fun submit(raw: String) {
        val code = WatchEsimHandoff.parse(raw)
        errorRes =
            when {
                code == null -> R.string.watch_esim_error_invalid
                code.needsConfirmationCode -> R.string.watch_esim_error_confirmation
                else -> null
            }
        if (code == null || errorRes != null) return
        try {
            context.startActivity(WatchEsimHandoff.createIntent(code))
            input = ""
            onDismissRequest()
        } catch (e: ActivityNotFoundException) {
            errorRes = R.string.watch_esim_error_companion_missing
        } catch (e: SecurityException) {
            errorRes = R.string.watch_esim_error_companion_missing
        }
    }

    fun scan() {
        HapticUtil.performUIHaptic(view)
        val options =
            GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()
        GmsBarcodeScanning.getClient(context, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue.orEmpty()
                input = value
                if (WatchEsimHandoff.parse(value) == null) {
                    errorRes = R.string.watch_esim_error_invalid
                } else {
                    errorRes = null
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, R.string.watch_esim_scan_unavailable, Toast.LENGTH_SHORT).show()
            }
    }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.watch_esim_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )

            RoundedCardContainer {
                Button(
                    onClick = { scan() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                    shape = segmentShape(0, 3),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_qr_code_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.watch_esim_scan_qr),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(segmentShape(1, 3))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(16.dp),
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            errorRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.watch_esim_enter_code)) },
                        placeholder = { Text("LPA:1\$smdp.example.com\$ABCD-1234") },
                        isError = errorRes != null,
                        supportingText = errorRes?.let { { Text(stringResource(it)) } },
                        singleLine = true,
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(segmentShape(2, 3))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            submit(input)
                        },
                        enabled = input.isNotBlank() && companionAvailable,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(text = stringResource(R.string.watch_esim_continue))
                    }
                    if (!companionAvailable) {
                        Text(
                            text = stringResource(R.string.watch_esim_error_companion_missing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.watch_esim_instructions_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )

            RoundedCardContainer {
                listOf(
                    R.string.watch_esim_step_1,
                    R.string.watch_esim_step_2,
                    R.string.watch_esim_step_3,
                    R.string.watch_esim_step_4,
                ).forEachIndexed { index, res ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(segmentShape(index, 5))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.watch_esim_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(segmentShape(4, 5))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(16.dp),
                )
            }
        }
    }
}

private fun segmentShape(
    index: Int,
    count: Int,
): RoundedCornerShape {
    val outer = 24.dp
    val inner = 4.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
}
