package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.TrackedRepo
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.TimeUtil
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrackedRepoCard(
    repo: TrackedRepo,
    isLoading: Boolean = false,
    installStatus: String? = null,
    downloadProgress: Float = 0f,
    onClick: () -> Unit,
    onShowReleaseNotes: () -> Unit = {}
) {
    val view = LocalView.current
    val context = LocalContext.current
    
    Card(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Icon + Badge
            Box(
                modifier = Modifier.size(56.dp)
            ) {
                // Main Image (App Icon or Android Icon)
                if (repo.mappedPackageName != null) {
                    val appIcon = remember(repo.mappedPackageName) {
                        try {
                            context.packageManager.getApplicationIcon(repo.mappedPackageName)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (appIcon != null) {
                        Image(
                            painter = BitmapPainter(
                                appIcon.toBitmap().asImageBitmap()
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_adb_24),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_adb_24),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Avatar Badge (User Profile)
                Surface(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceBright)
                ) {
                    AsyncImage(
                        model = repo.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                val isInstalled = repo.mappedPackageName != null
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isInstalled) repo.mappedAppName ?: repo.name else repo.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isInstalled) {
                        val cleanVersion = repo.latestTagName.removePrefix("v").removePrefix("V")
                        Text(
                            text = " $cleanVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Text(
                    text = if (isInstalled) repo.fullName else stringResource(R.string.label_no_app_linked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isInstalled) {
                    Text(
                        text = stringResource(R.string.format_updated_relative, TimeUtil.formatRelativeDate(repo.publishedAt, context)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            if (isLoading) {
                CircularWavyProgressIndicator()
            } else if (installStatus != null) {
                Column(
                    modifier = Modifier.width(64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CircularWavyProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (repo.isUpdateAvailable || repo.mappedPackageName == null) {
                IconButton(
                    onClick = {
                        HapticUtil.performMediumHaptic(view)
                        onClick()
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (repo.isUpdateAvailable) R.drawable.rounded_downloading_24 else R.drawable.rounded_download_24
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onShowReleaseNotes()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_release_alert_24),
                        contentDescription = stringResource(R.string.label_release_notes),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

