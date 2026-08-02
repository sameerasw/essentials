package com.sameerasw.essentials.translation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.StringLoader
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationBottomSheet(
    stringKey: String,
    initialTargetLocale: String? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentLocale = remember {
        val appLocale = context.resources.configuration.locales[0].language
        if (appLocale != "en" && appLocale.isNotBlank()) appLocale else initialTargetLocale ?: "si"
    }

    val translations = remember(stringKey) {
        StringLoader.getTranslationsForKey(context, stringKey)
    }

    val sourceEnglish = translations["en"] ?: ""
    val originalTargetVal = translations[currentLocale] ?: ""
    val currentDisplayVal = TranslationManager.getOverriddenText(stringKey, currentLocale, originalTargetVal)

    var inputText by remember { mutableStateOf(currentDisplayVal) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "Translate String",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Key: $stringKey",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Source & Target Cards wrapped in RoundedCardContainer
            RoundedCardContainer {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.translation_source_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    supportingContent = {
                        Text(
                            text = sourceEnglish.ifBlank { stringKey },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },

                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                )

                ListItem(
                    headlineContent = {
                        Text(
                            text = "Target Language (${currentLocale.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    supportingContent = {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                placeholder = { Text("Enter translation in ${currentLocale.uppercase()}…") },
                                singleLine = false,
                                maxLines = 4,
                                shape = MaterialTheme.shapes.large
                            )
                        }
                    },

                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismissRequest()
                    }
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        if (inputText.trim() != originalTargetVal.trim() && inputText.isNotBlank()) {
                            TranslationManager.addEdit(
                                key = stringKey,
                                locale = currentLocale,
                                originalValue = originalTargetVal,
                                newValue = inputText
                            )
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.translation_saved_toast),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            TranslationManager.removeEdit(stringKey, currentLocale)
                        }
                        onDismissRequest()

                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Edit")
                }
            }
        }
    }
}
