package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.CaffeinateViewModel

@Composable
fun CaffeinateSettingsUI(
    viewModel: CaffeinateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isActive = viewModel.isActive.value

    // Refresh state when composable is shown
    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

    }
}
