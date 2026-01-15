package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteriesSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.feat_batteries_desc),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        // Preview of the Battery Indicator
        androidx.compose.material3.CircularWavyProgressIndicator(
            progress = { 0.75f }, // Demo value
            modifier = Modifier.size(120.dp),
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            trackColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f)
        )
        
        Text(
            text = "Widget Preview Style",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
