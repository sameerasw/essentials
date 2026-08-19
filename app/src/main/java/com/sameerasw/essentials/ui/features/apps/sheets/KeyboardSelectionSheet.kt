package com.sameerasw.essentials.ui.features.apps.sheets

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun KeyboardSelectionSheet(
    onDismissRequest: (ime: String?) -> Unit,
    selectedIme: String?,
    context: Context = LocalContext.current
) {
    var isLoadingKeyboards by remember { mutableStateOf(true) }
    var defaultInputMethod by remember { mutableStateOf<String?>(null) }
    var imesList by remember { mutableStateOf<List<InputMethodInfo>>(emptyList()) }
    val view = LocalView.current
    val isDeprecated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    LaunchedEffect(Unit) {
        isLoadingKeyboards = true
        try {
            val list = withContext(Dispatchers.IO) {
                val imes =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val defaultIme =
                    if (isDeprecated)
                        imes.currentInputMethodInfo?.id
                    else
                        Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.DEFAULT_INPUT_METHOD
                        )
                defaultIme to imes
            }
            defaultInputMethod = selectedIme ?: (list.first ?: "")
            imesList = list.second.inputMethodList
        } catch (e: Exception) {
            Log.e(
                "KeyboardSelectionSheet", "Error loading input methods list: ${e.message ?: ""}"
            )
        } finally {
            isLoadingKeyboards = false
        }
    }

    EssentialsBottomSheet(
        onDismissRequest = { onDismissRequest(defaultInputMethod) },
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.diy_set_keyboard_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoadingKeyboards) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(imesList, key = { it.id }) { ime ->
                        val isEnabled = ime.serviceInfo.enabled
                        val isSelected = defaultInputMethod == ime.id

                        ListItem(
                            checked = isSelected,
                            onCheckedChange = {
                                if (isEnabled) {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    defaultInputMethod = ime.id
                                } else {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    Toast.makeText(
                                        context,
                                        R.string.diy_set_keyboard_input_method_disabled,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onLongClick = null,
                            enabled = isEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            leadingContent = {
                                Image(
                                    bitmap = ime.loadIcon(context.packageManager).toBitmap()
                                        .asImageBitmap(),
                                    contentDescription = ime.serviceInfo.name,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            },
                            supportingContent = null,
                            trailingContent = {
                                RadioButton(
                                    selected = if (isEnabled) isSelected else false,
                                    onClick = null,
                                    enabled = isEnabled
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 16.dp
                            ),
                            content = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = ime.loadLabel(context.packageManager).toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
