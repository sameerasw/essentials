package com.sameerasw.essentials.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.domain.diy.State as DIYState
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.utils.HapticUtil

class AutomationEditorActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_AUTOMATION_ID = "automation_id"
        private const val EXTRA_AUTOMATION_TYPE = "automation_type"

        fun createIntent(context: Context, automationId: String): Intent {
            return Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_ID, automationId)
            }
        }

        fun createIntent(context: Context, type: Automation.Type): Intent {
            return Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_TYPE, type.name)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init repository
        DIYRepository.init(applicationContext)

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID)
        val automationTypeStr = intent.getStringExtra(EXTRA_AUTOMATION_TYPE)
        
        val existingAutomation = if (automationId != null) DIYRepository.getAutomation(automationId) else null
        val isEditMode = existingAutomation != null

        val automationType = if (isEditMode) {
             existingAutomation?.type ?: Automation.Type.TRIGGER
        } else {
            try {
                Automation.Type.valueOf(automationTypeStr ?: Automation.Type.TRIGGER.name)
            } catch (e: Exception) {
                Automation.Type.TRIGGER
            }
        }
        
        val titleRes = if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title

        setContent {
            EssentialsTheme {
                val view = LocalView.current
                var carouselState = rememberCarouselState { 2 } // 0: Trigger/State, 1: Actions

                // State for selections
                // Initialize with existing data or defaults
                var selectedTrigger by remember { mutableStateOf<Trigger?>(existingAutomation?.trigger) }
                var selectedState by remember { mutableStateOf<DIYState?>(existingAutomation?.state) }
                
                // Actions
                // For Trigger type
                var selectedAction by remember { mutableStateOf<Action?>(existingAutomation?.actions?.firstOrNull()) }
                
                // For State type
                var selectedInAction by remember { mutableStateOf<Action?>(existingAutomation?.entryAction) }
                var selectedOutAction by remember { mutableStateOf<Action?>(existingAutomation?.exitAction) }
                
                // Tab for State Actions
                var selectedActionTab by remember { mutableIntStateOf(0) } // 0: In, 1: Out

                // Menu State
                var showMenu by remember { mutableStateOf(false) }

                // Validation
                val isValid = when (automationType) {
                    Automation.Type.TRIGGER -> selectedTrigger != null && selectedAction != null
                    Automation.Type.STATE -> selectedState != null && (selectedInAction != null || selectedOutAction != null)
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = titleRes,
                            hasBack = true,
                            isSmall = true,
                            onBackClick = { finish() },
                            actions = {
                                if (isEditMode) {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_more_vert_24),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    SegmentedDropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        SegmentedDropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_delete)) },
                                            onClick = {
                                                showMenu = false
                                                DIYRepository.removeAutomation(existingAutomation!!.id)
                                                finish()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.rounded_delete_24),
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                    ) {
                        HorizontalMultiBrowseCarousel(
                            state = carouselState,
                            preferredItemWidth = screenWidth,
                            itemSpacing = 4.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 18.dp)
                        ) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .maskClip(MaterialTheme.shapes.extraLarge)
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                if (index == 0) {
                                    // PAGE 0: Trigger or State Picker
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(if (automationType == Automation.Type.TRIGGER) R.string.diy_select_trigger else R.string.diy_select_state),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        
                                        RoundedCardContainer(spacing = 2.dp) {
                                            if (automationType == Automation.Type.TRIGGER) {
                                                val triggers = listOf(
                                                    Trigger.ScreenOff,
                                                    Trigger.ScreenOn,
                                                    Trigger.ScreenUnlock,
                                                    Trigger.ChargerConnected,
                                                    Trigger.ChargerDisconnected
                                                )
                                                triggers.forEach { trigger ->
                                                     EditorActionItem(
                                                        title = stringResource(trigger.title),
                                                        iconRes = trigger.icon,
                                                        isSelected = selectedTrigger == trigger,
                                                        onClick = { selectedTrigger = trigger }
                                                     )
                                                }
                                            } else {
                                                val states = listOf(
                                                    DIYState.Charging,
                                                    DIYState.ScreenOn
                                                )
                                                states.forEach { state ->
                                                     EditorActionItem(
                                                        title = stringResource(state.title),
                                                        iconRes = state.icon,
                                                        isSelected = selectedState == state,
                                                        onClick = { selectedState = state }
                                                     )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // PAGE 1: Action Picker
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.diy_select_action),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )

                                        if (automationType == Automation.Type.STATE) {
                                            // Tabs for In/Out
                                            val options = listOf(
                                                stringResource(R.string.diy_in_action_label),
                                                stringResource(R.string.diy_out_action_label)
                                            )
                                            SegmentedPicker(
                                                items = options,
                                                selectedItem = options[selectedActionTab],
                                                onItemSelected = { 
                                                    HapticUtil.performUIHaptic(view)
                                                    selectedActionTab = options.indexOf(it) 
                                                },
                                                labelProvider = { it },
                                                modifier = Modifier.fillMaxWidth(),
                                                cornerShape = MaterialTheme.shapes.extraExtraLarge.bottomEnd
                                            )
                                        }
                                        
                                        RoundedCardContainer(spacing = 2.dp) {
                                            val actions = listOf(
                                                Action.TurnOnFlashlight,
                                                Action.TurnOffFlashlight,
                                                Action.ToggleFlashlight,
                                                Action.HapticVibration
                                            )
                                            
                                            val currentSelection = when(automationType) {
                                                Automation.Type.TRIGGER -> selectedAction
                                                Automation.Type.STATE -> if (selectedActionTab == 0) selectedInAction else selectedOutAction
                                            }

                                            // None option
                                            EditorActionItem(
                                                title = stringResource(R.string.action_none),
                                                iconRes = R.drawable.rounded_do_not_disturb_on_24,
                                                isSelected = currentSelection == null,
                                                onClick = {
                                                    when(automationType) {
                                                        Automation.Type.TRIGGER -> selectedAction = null
                                                        Automation.Type.STATE -> {
                                                            if (selectedActionTab == 0) selectedInAction = null
                                                            else selectedOutAction = null
                                                        }
                                                    }
                                                }
                                            )

                                            actions.forEach { action ->
                                                 EditorActionItem(
                                                    title = stringResource(action.title),
                                                    iconRes = action.icon,
                                                    isSelected = currentSelection == action,
                                                    onClick = { 
                                                        when(automationType) {
                                                            Automation.Type.TRIGGER -> selectedAction = action
                                                            Automation.Type.STATE -> {
                                                                if (selectedActionTab == 0) selectedInAction = action
                                                                else selectedOutAction = action
                                                            }
                                                        }
                                                    }
                                                 )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    finish()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_close_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(R.string.action_cancel))
                            }

                            Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    // Save logic
                                    if (automationType == Automation.Type.TRIGGER) {
                                        val newAutomation = Automation(
                                            id = if (isEditMode) existingAutomation!!.id else java.util.UUID.randomUUID().toString(),
                                            type = Automation.Type.TRIGGER,
                                            trigger = selectedTrigger,
                                            actions = listOfNotNull(selectedAction)
                                        )
                                        if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                                    } else {
                                        val newAutomation = Automation(
                                            id = if (isEditMode) existingAutomation!!.id else java.util.UUID.randomUUID().toString(),
                                            type = Automation.Type.STATE,
                                            state = selectedState,
                                            entryAction = selectedInAction,
                                            exitAction = selectedOutAction
                                        )
                                        if (isEditMode) DIYRepository.updateAutomation(newAutomation) else DIYRepository.addAutomation(newAutomation)
                                    }
                                    finish()
                                },
                                modifier = Modifier.weight(1f),
                                enabled = isValid
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_check_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(R.string.action_save))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorActionItem(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                HapticUtil.performUIHaptic(view)
                onClick() 
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
