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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
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

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID)
        val automationTypeStr = intent.getStringExtra(EXTRA_AUTOMATION_TYPE)
        
        val isEditMode = automationId != null
        val automationType = if (isEditMode) {
             if (automationId == "2") Automation.Type.STATE else Automation.Type.TRIGGER
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
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = titleRes,
                            hasBack = true,
                            isSmall = true,
                            onBackClick = { finish() }
                        )
                    }
                ) { innerPadding ->
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val carouselState = rememberCarouselState { 2 } // 0: Trigger/State, 1: Actions

                    // State for selections
                    var selectedTriggerId by remember { mutableStateOf("screen_off") }
                    var selectedStateId by remember { mutableStateOf("wifi_connected") }
                    
                    // Actions
                    var selectedActionId by remember { mutableStateOf<String?>("flashlight") } // For Trigger automation
                    var selectedInActionId by remember { mutableStateOf<String?>("flashlight") }
                    var selectedOutActionId by remember { mutableStateOf<String?>("vibrate") }
                    
                    // Tab for State Actions
                    var selectedActionTab by remember { mutableIntStateOf(0) } // 0: In, 1: Out

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
                                            text = if (automationType == Automation.Type.TRIGGER) "Select Trigger" else "Select State",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        
                                        RoundedCardContainer(spacing = 2.dp) {
                                            if (automationType == Automation.Type.TRIGGER) {
                                                // Mock Triggers
                                                val triggers = listOf(
                                                    Triple("screen_off", "Screen Off", R.drawable.rounded_mobile_cancel_24),
                                                    Triple("screen_on", "Screen On", R.drawable.rounded_mobile_text_2_24),
                                                    Triple("power_connected", "Power Connected", R.drawable.rounded_battery_charging_60_24)
                                                )
                                                triggers.forEach { (id, name, icon) ->
                                                     EditorActionItem(
                                                        title = name,
                                                        iconRes = icon,
                                                        isSelected = selectedTriggerId == id,
                                                        onClick = { selectedTriggerId = id }
                                                     )
                                                }
                                            } else {
                                                // Mock States
                                                val states = listOf(
                                                    Triple("wifi_connected", "Wifi Connected", R.drawable.rounded_android_wifi_3_bar_24),
                                                    Triple("bluetooth_connected", "Bluetooth Connected", R.drawable.rounded_bluetooth_24),
                                                    Triple("dnd_active", "DND Active", R.drawable.rounded_do_not_disturb_on_24)
                                                )
                                                states.forEach { (id, name, icon) ->
                                                     EditorActionItem(
                                                        title = name,
                                                        iconRes = icon,
                                                        isSelected = selectedStateId == id,
                                                        onClick = { selectedStateId = id }
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
                                            text = "Select Action",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )

                                        if (automationType == Automation.Type.STATE) {
                                            // Tabs for In/Out
                                            val options = listOf("In Action", "Out Action")
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
                                            // Mock Actions
                                            val actions = listOf(
                                                Triple("flashlight", "Toggle Flashlight", R.drawable.rounded_flashlight_on_24),
                                                Triple("vibrate", "Toggle Vibrate", R.drawable.rounded_mobile_vibrate_24),
                                                Triple("media_play_pause", "Media Play/Pause", R.drawable.rounded_play_pause_24),
                                                Triple("none", "None", R.drawable.rounded_do_not_disturb_on_24) // Allow none/clear
                                            )
                                            
                                            val currentSelection = when(automationType) {
                                                Automation.Type.TRIGGER -> selectedActionId
                                                Automation.Type.STATE -> if (selectedActionTab == 0) selectedInActionId else selectedOutActionId
                                            }

                                            actions.forEach { (id, name, icon) ->
                                                 EditorActionItem(
                                                    title = name,
                                                    iconRes = icon,
                                                    isSelected = currentSelection == id,
                                                    onClick = { 
                                                        when(automationType) {
                                                            Automation.Type.TRIGGER -> selectedActionId = id
                                                            Automation.Type.STATE -> {
                                                                if (selectedActionTab == 0) selectedInActionId = id
                                                                else selectedOutActionId = id
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
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_close_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    finish()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_check_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Save")
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
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
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
