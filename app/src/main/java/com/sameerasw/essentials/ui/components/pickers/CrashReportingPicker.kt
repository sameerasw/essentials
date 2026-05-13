package com.sameerasw.essentials.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CrashReportingPicker(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int = R.drawable.rounded_bug_report_24
) {
    val options = listOf("off", "auto")
    val labels = listOf(
        R.string.sentry_mode_off,
        R.string.sentry_mode_auto
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ListItem(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            content = {
                Text(
                    text = stringResource(R.string.sentry_report_mode_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { index, option ->
                val isChecked = selectedMode == option

                ToggleButton(
                    checked = isChecked,
                    onCheckedChange = {
                        onModeSelected(option)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shapes = when {
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Text(
                        text = stringResource(labels[index]),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
