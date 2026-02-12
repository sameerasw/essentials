package com.sameerasw.essentials.ui.components.containers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.linkActions.AppPickerItem
import com.sameerasw.essentials.ui.components.linkActions.ResolvedAppInfo

@Composable
fun RoundedCardLazyContainer(
    modifier: Modifier = Modifier,
    resolveInfos: List<ResolvedAppInfo>,
    spacing: Dp = 2.dp,
    cornerRadius: Dp = 24.dp,
    uri: Uri,
    onFinish: () -> Unit,
    actionType: String = Intent.ACTION_SEND,
    togglePin: (String) -> Unit,
    pinnedPackages: Set<String>,
    demo: Boolean = false
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius)),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(resolveInfos) { info ->

            AppPickerItem(
                info = info,
                togglePin = togglePin,
                pinnedPackages = pinnedPackages,
                demo = demo,
                onTapAction = {
                    val intent = if (actionType == Intent.ACTION_VIEW) {
                        Intent(Intent.ACTION_VIEW, uri)
                    } else {
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, uri.toString())
                        }
                    }
                    intent.setClassName(
                        info.resolveInfo.activityInfo.packageName,
                        info.resolveInfo.activityInfo.name
                    )
                    context.startActivity(intent)
                    onFinish()
                }
            )
        }
    }
}
