package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.cards.PermissionCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.buttons.HelpPillButton

data class PermissionItem(
    val iconRes: Int,
    val title: String,
    val description: String,
    val dependentFeatures: List<String> = emptyList(),
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
    val secondaryActionLabel: String? = null,
    val secondaryAction: (() -> Unit)? = null,
    val isGranted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    onDismissRequest: () -> Unit,
    featureTitle: String,
    permissions: List<PermissionItem>
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$featureTitle requires following permissions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                HelpPillButton()
            }

            RoundedCardContainer() {
                permissions.forEach { perm ->
                    PermissionCard(
                        iconRes = perm.iconRes,
                        title = perm.title,
                        dependentFeatures = perm.dependentFeatures,
                        actionLabel = perm.actionLabel ?: "Open Settings",
                        isGranted = perm.isGranted,
                        onActionClick = { perm.action?.invoke() },
                        secondaryActionLabel = perm.secondaryActionLabel,
                        onSecondaryActionClick = { perm.secondaryAction?.invoke() }
                    )
                }
            }
        }
    }
}
