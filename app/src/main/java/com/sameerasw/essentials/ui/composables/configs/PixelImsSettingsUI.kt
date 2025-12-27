package com.sameerasw.essentials.ui.composables.configs

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import androidx.core.net.toUri

@Composable
fun PixelImsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isConfirmed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Toggle
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(16.dp)
                    .clickable(enabled = !isConfirmed && !viewModel.isPixelImsEnabled.value) {
                        Toast.makeText(context, "Please read and understand instructions", Toast.LENGTH_SHORT).show()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_wifi_calling_bar_3_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isConfirmed || viewModel.isPixelImsEnabled.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Force enable IMS",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConfirmed || viewModel.isPixelImsEnabled.value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
                Switch(
                    checked = viewModel.isPixelImsEnabled.value,
                    enabled = isConfirmed || viewModel.isPixelImsEnabled.value,
                    onCheckedChange = { checked ->
                        if (checked && !viewModel.isShizukuPermissionGranted.value) {
                            viewModel.requestShizukuPermission()
                        } else {
                            viewModel.setPixelImsEnabled(checked, context)
                        }
                    }
                )
            }
        }

        // Warning Section
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⚠️ WARNING",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "This feature is only intended for Tensor Pixel devices (Pixel 6 and later) to force enable IMS in unsupported countries to gain VoLTE features with the carriers. Please do not modify this setting on non-Pixel devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Also keep in mind that any damage or malfunction this feature causes to your device, the developers of this app or the initial IMS developers do not take responsibility, so proceed at your own risk.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "There is a change Google breaking this feature with future updates as they've been trying to. Hope it won't happen :)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isConfirmed = !isConfirmed },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isConfirmed,
                        onCheckedChange = { isConfirmed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.error,
                            uncheckedColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = "I understand the risks and instructions",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }


        // Warning Section
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
            cornerRadius = 20.dp
        ) {
            // Info Section
            Text(
                text = "Before proceeding, make sure your device is up-to-date and re-check to confirm that your carrier does not support your device with VoLTE and IMS. Check for the VoLTE or 4G calling toggles in the SIM settings and if they already exist, contact your carrier to verify if they can help or not. If not, only then continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            )

            // Instructions
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InstructionItem(
                    1,
                    "Make sure you remove any other IMS/VoLTE activation tools before starting and go into Mobile network settings, reset mobile networks and then restart the device."
                )
                InstructionItem(
                    2,
                    "Grant Shizuku to this app and enable the 'Force IMS' toggle above."
                )
                InstructionItem(
                    3,
                    "Go into Settings > Network and Internet > SIMs > Your SIM > Turn on VoLTE or 4G calling toggles. If not present, try after a restart."
                )
                InstructionItem(4, "Restart the device.")
                InstructionItem(
                    5,
                    "Go into the below screen and check if IMS is registered for your SIM. You might have to contact your carrier and enable from their end as well."
                )
            }

            // Hidden Menu Button
            Button(
                onClick = {
                    try {
                        val intent = Intent("android.intent.action.MAIN")
                        intent.component = ComponentName(
                            "com.android.phone",
                            "com.android.phone.settings.hiddenmenu.PhoneInformationV2"
                        )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("PixelIms", "Failed to launch phone info", e)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Phone Information")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Credits
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Based on the original project",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "vvb2060/Ims",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable {
                        val intent =
                            Intent(Intent.ACTION_VIEW, "https://github.com/vvb2060/Ims".toUri())
                        context.startActivity(intent)
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InstructionItem(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
