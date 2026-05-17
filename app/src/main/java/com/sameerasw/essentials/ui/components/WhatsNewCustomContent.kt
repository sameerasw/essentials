package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded

/**
 * Slot for custom content to be displayed in the "What's New" screen.
 */
@Composable
fun WhatsNewCustomContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Feature 1: Specs Retired
        WhatsNewItemCard(
            title = stringResource(R.string.whats_new_specs_title),
            desc = stringResource(R.string.whats_new_specs_desc),
            iconRes = R.drawable.rounded_close_24,
            iconTint = MaterialTheme.colorScheme.error,
            iconBackground = MaterialTheme.colorScheme.errorContainer
        )

        // Feature 2: Apps Migrated
        WhatsNewItemCard(
            title = stringResource(R.string.whats_new_apps_title),
            desc = stringResource(R.string.whats_new_apps_desc),
            iconRes = R.drawable.rounded_apps_24,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
fun WhatsNewItemCard(
    title: String,
    desc: String,
    iconRes: Int,
    iconTint: Color,
    iconBackground: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
                color = iconBackground
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
