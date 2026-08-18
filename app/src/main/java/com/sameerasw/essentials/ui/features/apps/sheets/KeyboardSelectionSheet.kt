package com.sameerasw.essentials.ui.features.apps.sheets

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
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
                "KeyboardSelectionSheet", "Error loading keyboards list: ${e.message ?: ""}"
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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Keyboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (PermissionUtils.canWriteSecureSettings(context)) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_shield_lock_24),
                        contentDescription = "Permission",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(imesList, key = { _, ime -> ime.id }) { index, ime ->
                        val isEnabled = ime.serviceInfo.enabled
                        val isSelected = defaultInputMethod == ime.id

                        val currentIndex =
                            if (imesList.size == 1) -2 else if (index == (imesList.size - 1)) -1 else index
                        val shape = when (currentIndex) {
                            -2 -> RoundedCornerShape(24.dp)
                            -1 -> RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp)
                            0 -> RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp)
                            else -> RoundedCornerShape(4.dp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .clickable {
                                    HapticUtil.performUIHaptic(view)
                                    if (isEnabled) {
                                        defaultInputMethod = ime.id
                                        return@clickable
                                    }
                                    Toast.makeText(
                                        context,
                                        "Enable in settings",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = ime.loadIcon(context.packageManager).toBitmap()
                                    .asImageBitmap(),
                                contentDescription = "IME Logo",
                                modifier = Modifier.size(24.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = ime.loadLabel(context.packageManager).toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { defaultInputMethod = ime.id },
                                enabled = isEnabled
                            )
                        }
                    }
                }
            }
        }
    }
}
