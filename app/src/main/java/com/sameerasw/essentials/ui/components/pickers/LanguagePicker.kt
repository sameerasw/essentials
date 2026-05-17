package com.sameerasw.essentials.ui.components.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.LanguageUtils

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanguagePicker(
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = LanguageUtils.languages
    val selectedLanguage = languages.find { it.code == selectedLanguageCode } ?: languages.first()

    ConfigPickerItem(
        title = stringResource(R.string.label_app_language),
        selectedValue = "${selectedLanguage.nativeName} (${selectedLanguage.name})",
        iconRes = R.drawable.rounded_globe_24,
        modifier = modifier.fillMaxWidth()
    ) {
        languages.forEach { language ->
            SegmentedDropdownMenuItem(
                text = {
                    Text(text = "${language.nativeName} (${language.name})")
                },
                onClick = {
                    onLanguageSelected(language.code)
                }
            )
        }
    }
}
