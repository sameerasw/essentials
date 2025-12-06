package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R

@Composable
fun FeatureCard(
    title: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    hasMoreSettings: Boolean = true,
    isToggleEnabled: Boolean = true,
    onDisabledToggleClick: (() -> Unit)? = null,
    description: String? = null
) {
    val view = LocalView.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.clickable {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        }) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {

            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (iconRes != null) {

                    Spacer(modifier = Modifier.size(1.dp))
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(1.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = title)
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (hasMoreSettings) {
                    Icon(
                        modifier = Modifier.padding(end = 12.dp).size(24.dp),
                        painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                        contentDescription = "More settings"
                    )
                }

                Box {
                    Switch(
                        checked = if (isToggleEnabled) isEnabled else false,
                        onCheckedChange = { checked ->
                            if (isToggleEnabled) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                onToggle(checked)
                            }
                        },
                        enabled = isToggleEnabled
                    )

                    if (!isToggleEnabled && onDisabledToggleClick != null) {
                        // Invisible overlay catches taps even if the child consumes them
                        Box(modifier = Modifier.matchParentSize().clickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            onDisabledToggleClick()
                        })
                    }
                }
            }
        }
    }
}
