package com.sameerasw.essentials.ui.components.linkActions

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

@Composable
fun ShareWithContent(
    resolveInfos: List<ResolvedAppInfo>,
    uri: Uri,
    onFinish: () -> Unit,
    modifier: Modifier,
    togglePin: (String) -> Unit,
    pinnedPackages: Set<String>,
    demo: Boolean = false,
    topPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
) {
    Log.d("LinkPicker", "ShareWithContent: ${resolveInfos.size} apps found")
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(topPadding))

        if (resolveInfos.isEmpty()) {
            Text(
                text = "No apps found to share with",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            RoundedCardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                resolveInfos.forEach { info ->
                    AppPickerItem(
                        info = info,
                        togglePin = togglePin,
                        pinnedPackages = pinnedPackages,
                        demo = demo,
                        onTapAction = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, uri.toString())
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

        Spacer(modifier = Modifier.height(300.dp))
    }
}
