package com.sameerasw.essentials.ui.components.sheets

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.UpdateInfo
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.text.SimpleMarkdown
import com.sameerasw.essentials.utils.AutoUpdateManagerHelper
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateBottomSheet(
    updateInfo: UpdateInfo?,
    isChecking: Boolean,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    var fetchedNotes by remember(updateInfo?.releaseNotes) { mutableStateOf<String?>(null) }
    var isFetchingNotes by remember(updateInfo?.releaseNotes) { mutableStateOf(false) }

    LaunchedEffect(updateInfo?.releaseNotes) {
        val notesStr = updateInfo?.releaseNotes?.trim() ?: ""
        if (notesStr.startsWith("http://") || notesStr.startsWith("https://")) {
            isFetchingNotes = true
            fetchedNotes = withContext(Dispatchers.IO) {
                fetchMarkdownFromUrl(notesStr)
            }
            isFetchingNotes = false
        } else {
            fetchedNotes = notesStr
            isFetchingNotes = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isChecking || updateInfo == null) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (updateInfo.isUpdateAvailable) R.drawable.rounded_mobile_arrow_down_24 else R.drawable.rounded_mobile_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (updateInfo.isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        text = if (updateInfo.isUpdateAvailable) stringResource(R.string.update_available_title) else stringResource(
                            R.string.status_up_to_date
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val isPreRelease = remember(updateInfo.versionName) {
                        val v = updateInfo.versionName.lowercase()
                        v.contains("beta") || v.contains("alpha") || v.contains("rc") || v.contains(
                            "pre"
                        )
                    }

                    if (isPreRelease) {
                        RoundedCardContainer {
                            ListItem(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_mobile_code_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 16.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                        alpha = 0.5f
                                    )
                                ),
                                content = {
                                    Text(
                                        text = stringResource(R.string.warning_pre_release),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val notesToDisplay = fetchedNotes ?: updateInfo.releaseNotes
                    if (isFetchingNotes) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LoadingIndicator()
                        }
                    } else if (notesToDisplay.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.release_notes_format,
                                    updateInfo.versionName
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            RoundedCardContainer {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    SimpleMarkdown(
                                        content = notesToDisplay,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (updateInfo.releaseUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                val intent =
                                    Intent(Intent.ACTION_VIEW, updateInfo.releaseUrl.toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.brand_github),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_view_on_github))
                        }
                    }

                    if (updateInfo.isUpdateAvailable && updateInfo.downloadUrl.isNotEmpty()) {
                        if (isDownloading) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearWavyProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.downloading_update_progress, downloadProgress),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    isDownloading = true
                                    val helper = AutoUpdateManagerHelper(context)
                                    coroutineScope.launch {
                                        val cleanVersion = updateInfo.versionName.replace(Regex("[^a-zA-Z0-9]"), "_")
                                        helper.downloadAndInstallApk(
                                            apkUrl = updateInfo.downloadUrl,
                                            apkName = "Essentials_$cleanVersion",
                                            onProgressUpdate = { progress ->
                                                downloadProgress = progress
                                                if (progress >= 100) {
                                                    isDownloading = false
                                                }
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_mobile_arrow_down_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.action_install_update))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun fetchMarkdownFromUrl(urlStr: String): String {
    return try {
        var targetUrl = urlStr
        val gitHubReleaseRegex = Regex("https?://github\\.com/([^/]+)/([^/]+)/releases/tag/(.+)")
        val match = gitHubReleaseRegex.matchEntire(urlStr)
        if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2]
            val tag = match.groupValues[3]
            targetUrl = "https://api.github.com/repos/$owner/$repo/releases/tags/$tag"
        }

        val url = URL(targetUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json, text/plain, */*")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        if (conn.responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            try {
                val jsonObject = Gson().fromJson(responseText, JsonObject::class.java)
                if (jsonObject.has("body")) {
                    return jsonObject.get("body").asString
                }
            } catch (_: Exception) {
            }
            responseText
        } else {
            ""
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}
