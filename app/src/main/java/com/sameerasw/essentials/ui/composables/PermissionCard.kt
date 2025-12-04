package com.sameerasw.essentials.ui.composables

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCard(
    iconRes: Int,
    title: String,
    dependentFeatures: List<String>,
    actionLabel: String,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    val grantedGreen = Color(0xFF4CAF50)

    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Spacer(modifier = Modifier.size(12.dp))

                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (isGranted) grantedGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.size(24.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)

                        if (isGranted) {
                            Spacer(modifier = Modifier.size(8.dp))
                            // Granted chip
                            Box(
                                modifier = Modifier
                                    .background(color = grantedGreen, shape = MaterialTheme.shapes.small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Granted", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Required for:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Bulleted list of dependent features
                    dependentFeatures.forEach { f ->
                        Text(text = "• $f", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (isGranted) {
                OutlinedButton(onClick = onActionClick, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(painter = painterResource(id = com.sameerasw.essentials.R.drawable.rounded_arrow_forward_24), contentDescription = null)
                }
            } else {
                // Show buttons - either single or dual buttons
                if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onActionClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(actionLabel)
                        }

                        Button(
                            onClick = onSecondaryActionClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(secondaryActionLabel)
                        }
                    }
                } else {
                    Button(onClick = onActionClick, modifier = Modifier.fillMaxWidth()) {
                        Text(actionLabel)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(painter = painterResource(id = com.sameerasw.essentials.R.drawable.rounded_arrow_forward_24), contentDescription = null)
                    }
                }
            }
        }
    }
}
