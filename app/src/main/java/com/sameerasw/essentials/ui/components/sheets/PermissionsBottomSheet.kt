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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.PermissionCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

data class PermissionItem(
    val iconRes: Int,
    val title: Any, // Can be Int (Resource ID) or String
    val description: Any, // Can be Int or String
    val dependentFeatures: List<Any> = emptyList(), // List of Int or String
    val actionLabel: Any? = null, // Can be Int or String
    val action: (() -> Unit)? = null,
    val secondaryActionLabel: Any? = null, // Can be Int or String
    val secondaryAction: (() -> Unit)? = null,
    val isGranted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsBottomSheet(
    onDismissRequest: () -> Unit,
    featureTitle: Any, // Can be Int (Resource ID) or String
    permissions: List<PermissionItem>,
    onHelpClick: () -> Unit = {}
) {
    val resolvedTitle = when (featureTitle) {
        is Int -> stringResource(id = featureTitle)
        is String -> featureTitle
        else -> ""
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        id = R.string.requires_following_permissions,
                        resolvedTitle
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            RoundedCardContainer {
                permissions.forEach { perm ->
                    PermissionCard(
                        iconRes = perm.iconRes,
                        title = perm.title,
                        dependentFeatures = perm.dependentFeatures,
                        actionLabel = perm.actionLabel ?: R.string.perm_action_enable,
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
