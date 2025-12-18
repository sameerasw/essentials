package com.sameerasw.essentials.ui

import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.containers.RoundedCardLazyContainer

@Composable
fun OpenWithContent(
    resolveInfos: List<ResolveInfo>,
    uri: Uri,
    onFinish: () -> Unit,
    modifier: Modifier,
    togglePin: (String) -> Unit,
    pinnedPackages: Set<String>
) {
    Log.d("LinkPicker", "OpenWithContent: ${resolveInfos.size} apps found")

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (resolveInfos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Text(
                    text = "No apps found to open this link",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            RoundedCardLazyContainer(
                resolveInfos = resolveInfos,
                uri = uri,
                onFinish = onFinish,
                actionType = android.content.Intent.ACTION_VIEW,
                togglePin = togglePin,
                pinnedPackages = pinnedPackages
            )
        }
    }
}
