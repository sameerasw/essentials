package com.sameerasw.essentials.ui.core.sheets

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.configshare.ConfigParseError
import com.sameerasw.essentials.domain.configshare.ConfigParseResult
import com.sameerasw.essentials.domain.configshare.ConfigShareDescriptor
import com.sameerasw.essentials.domain.configshare.ConfigShareManager
import com.sameerasw.essentials.domain.configshare.ParsedConfig
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun ConfigShareRow(
    descriptor: ConfigShareDescriptor,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showShare by remember { mutableStateOf(false) }
    var parsed by remember { mutableStateOf<ParsedConfig?>(null) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val text =
                try {
                    context.contentResolver.openInputStream(uri)?.let { ConfigShareManager.readLimited(it) }
                } catch (e: Exception) {
                    ""
                }
            when (val result = ConfigShareManager.parse(context, descriptor, text)) {
                is ConfigParseResult.Success -> parsed = result.config
                is ConfigParseResult.Failure ->
                    Toast
                        .makeText(
                            context,
                            when (result.error) {
                                ConfigParseError.TOO_LARGE -> R.string.config_import_error_too_large
                                ConfigParseError.WRONG_FEATURE -> R.string.config_import_error_wrong_feature
                                ConfigParseError.NEWER_SCHEMA -> R.string.config_import_error_newer_schema
                                ConfigParseError.EMPTY -> R.string.config_import_error_empty
                                ConfigParseError.INVALID -> R.string.config_import_error_invalid
                            },
                            Toast.LENGTH_LONG,
                        ).show()
            }
        }

    if (showShare) {
        ConfigShareSheet(descriptor = descriptor, onDismissRequest = { showShare = false })
    }
    parsed?.let { config ->
        ConfigImportSheet(
            descriptor = descriptor,
            config = config,
            onDismissRequest = { parsed = null },
            onApplied = onApplied,
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(MaterialTheme.colorScheme.surfaceBright),
            contentAlignment = Alignment.CenterStart,
        ) {
            TranslatableText(
                stringResId = R.string.config_share_row_title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        FilledTonalIconButton(
            onClick = {
                HapticUtil.performUIHaptic(view)
                importLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier.size(width = 58.dp, height = 58.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_vertical_align_bottom_24),
                contentDescription = stringResource(R.string.config_share_action_import),
            )
        }
        FilledIconButton(
            onClick = {
                HapticUtil.performUIHaptic(view)
                showShare = true
            },
            modifier = Modifier.size(width = 58.dp, height = 58.dp),
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_share_24),
                contentDescription = stringResource(R.string.config_share_action_share),
            )
        }
    }
}
