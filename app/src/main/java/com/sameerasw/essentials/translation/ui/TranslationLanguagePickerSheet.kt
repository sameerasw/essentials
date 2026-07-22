package com.sameerasw.essentials.translation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationLanguagePickerSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val availableLocales = remember {
        listOf(
            "en" to "English",
            "es" to "Spanish (Español)",
            "fr" to "French (Français)",
            "de" to "German (Deutsch)",
            "it" to "Italian (Italiano)",
            "pt" to "Portuguese (Português)",
            "ru" to "Russian (Русский)",
            "zh" to "Chinese (中文)",
            "ja" to "Japanese (日本語)",
            "ko" to "Korean (한국어)",
            "ar" to "Arabic (العربية)",
            "hi" to "Hindi (हिन्दी)",
            "si" to "Sinhala (සිංහල)",
            "tr" to "Turkish (Türkçe)",
            "nl" to "Dutch (Nederlands)",
            "pl" to "Polish (Polski)",
            "sv" to "Swedish (Svenska)",
            "uk" to "Ukrainian (Українська)",
            "vi" to "Vietnamese (Tiếng Việt)"
        )
    }

    val selected = remember {
        mutableStateListOf<String>().apply {
            addAll(TranslationManager.selectedLanguages)
        }
    }

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
            Text(
                text = "Select Target Languages (Max 5)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${selected.size}/5 languages selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(availableLocales) { (code, name) ->
                    val isChecked = selected.contains(code)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            if (isChecked) {
                                selected.remove(code)
                            } else if (selected.size < 5) {
                                selected.add(code)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = code.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    HapticUtil.performUIHaptic(view)
                                    if (checked && selected.size < 5) {
                                        selected.add(code)
                                    } else {
                                        selected.remove(code)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        TranslationManager.setSelectedLanguages(selected)
                        onDismissRequest()
                    }
                ) {
                    Text("Done")
                }
            }
        }
    }
}
