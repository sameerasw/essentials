package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.ui.components.diy.AutomationItem
import com.sameerasw.essentials.viewmodels.DIYViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.activities.AutomationEditorActivity
import com.sameerasw.essentials.ui.components.sheets.NewAutomationSheet

@Composable
fun DIYScreen(
    modifier: Modifier = Modifier,
    viewModel: DIYViewModel = viewModel()
) {
    val context = LocalContext.current
    val automations by viewModel.automations.collectAsState()
    val focusManager = LocalFocusManager.current
    
    var showNewAutomationSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (automations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No automations yet"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(automations) { automation ->
                        AutomationItem(
                            automation = automation,
                            onClick = {
                                context.startActivity(AutomationEditorActivity.createIntent(context, automation.id))
                            }
                        )
                    }
                }
            }
        }
        
        // FAB
        FloatingActionButton(
            onClick = { showNewAutomationSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 32.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_add_24),
                contentDescription = stringResource(R.string.diy_editor_new_title)
            )
        }
    }

    if (showNewAutomationSheet) {
        NewAutomationSheet(
            onDismiss = { showNewAutomationSheet = false },
            onOptionSelected = { type ->
                showNewAutomationSheet = false
                context.startActivity(AutomationEditorActivity.createIntent(context, type))
            }
        )
    }
}
