/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: MeDropButtonCard.kt
 */

package com.sameerasw.essentials.ui.core.cards

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.features.system.sheets.MeDropBottomSheet
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.MeDropContactPickerHelper
import com.sameerasw.essentials.utils.MeDropNfcManager
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MeDropButtonCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val mainViewModel: MainViewModel = viewModel()
    
    val showMeDropSheet by mainViewModel.showMeDropSheet
    val currentContact by mainViewModel.meDropContact

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    val contact = MeDropContactPickerHelper.processResult(uri, context)
                    mainViewModel.setMeDropContact(context, contact)
                }
            }
        }
    }

    LaunchedEffect(showMeDropSheet, currentContact) {
        val activity = context as? Activity
        if (showMeDropSheet && currentContact != null && activity != null) {
            MeDropNfcManager.startBroadcast(activity, currentContact!!)
        } else if (activity != null) {
            MeDropNfcManager.stopBroadcast(activity)
        }
    }

    if (showMeDropSheet) {
        MeDropBottomSheet(
            viewModel = mainViewModel,
            onPickContact = {
                contactPickerLauncher.launch(MeDropContactPickerHelper.buildPickIntent())
            },
            onDismissRequest = { mainViewModel.showMeDropSheet.value = false }
        )
    }

    FilledTonalButton(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            mainViewModel.showMeDropSheet.value = true
        },
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.medium
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_contacts_product_24),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.feat_medrop_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
