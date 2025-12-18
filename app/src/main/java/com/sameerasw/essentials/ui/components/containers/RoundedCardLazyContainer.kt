package com.sameerasw.essentials.ui.components.containers

import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.sameerasw.essentials.ui.AppPickerItem

@Composable
fun RoundedCardLazyContainer(
    modifier: Modifier = Modifier,
    resolveInfos: List<ResolveInfo>,
    spacing: Dp = 2.dp,
    cornerRadius: Dp = 24.dp,
    uri: Uri,
    onFinish: () -> Unit,
    actionType: String = Intent.ACTION_SEND,
    togglePin: (String) -> Unit,
    pinnedPackages: Set<String>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(resolveInfos) { resolveInfo ->

            AppPickerItem(
                resolveInfo = resolveInfo,
                onClick = {
                    val intent = if (actionType == Intent.ACTION_VIEW) {
                        Intent(Intent.ACTION_VIEW, uri)
                    } else {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, uri.toString())
                        }
                    }
                    intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
                    context.startActivity(intent)
                    onFinish()
                },
                togglePin = togglePin,
                pinnedPackages = pinnedPackages
            )
        }
    }
}
