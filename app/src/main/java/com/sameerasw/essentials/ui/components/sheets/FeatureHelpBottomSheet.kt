package com.sameerasw.essentials.ui.components.sheets

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.ColorUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureHelpBottomSheet(
    onDismissRequest: () -> Unit,
    feature: Feature
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Header with Icon and Title
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = ColorUtil.getPastelColorFor(stringResource(feature.title)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = feature.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = ColorUtil.getVibrantColorFor(stringResource(feature.title))
                    )
                }
                
                Column {
                    Text(
                        text = stringResource(feature.title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Description Body
            RoundedCardContainer(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val description = if (feature.aboutDescription != null) {
                        stringResource(feature.aboutDescription!!)
                    } else {
                        stringResource(feature.description)
                    }
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
