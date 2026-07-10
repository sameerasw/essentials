package com.sameerasw.essentials.ui.activities

import com.sameerasw.essentials.utils.TelephonyParser
import com.sameerasw.essentials.utils.SimInfo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class NetworkModeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                NetworkModeSettingsOverlay(onDismiss = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkModeSettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeSims = remember { TelephonyParser.parseActiveSims(ShellUtils.runCommandWithOutput(context, "dumpsys isub") ?: "") }
    val currentSelectedModes = remember { mutableStateMapOf<Int, NetworkModeOption>() }
    var defaultDataSubId by remember { mutableStateOf(getDefaultDataSubIdFromShell(context)) }

    // Load initial network modes
    LaunchedEffect(Unit) {
        activeSims.forEach { sim ->
            currentSelectedModes[sim.id] = getCurrentNetworkMode(context, sim.id, sim.slotIndex)
        }
    }

    // Register receiver to listen to SIM changes in real-time
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            addAction("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                defaultDataSubId = getDefaultDataSubIdFromShell(context)
                activeSims.forEach { sim ->
                    currentSelectedModes[sim.id] = getCurrentNetworkMode(context, sim.id, sim.slotIndex)
                }
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_android_cell_dual_4_bar_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Telephony Controller",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dual SIM & Network Controller",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (activeSims.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No active SIM cards found.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Section 1: Default Mobile Data SIM Switch
                RoundedCardContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Data SIM",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeSims.forEach { sim ->
                                val isActive = sim.id == defaultDataSubId
                                Button(
                                    onClick = {
                                        if (!isActive) {
                                            switchDefaultDataSubId(context, sim.id)
                                            HapticUtil.performHeavyHaptic(view)
                                        }
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = sim.displayName)
                                }
                            }
                        }
                    }
                }

                // Section 2: Network Band Locking per SIM
                activeSims.forEach { sim ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SIM - ${sim.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        val currentMode = currentSelectedModes[sim.id] ?: NetworkModeOption.MODE_5G_PREF
                        var showDialog by remember { mutableStateOf(false) }

                        RoundedCardContainer {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Preferred Network Mode",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap to change configuration",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (currentMode) {
                                            NetworkModeOption.MODE_5G_PREF -> "5G Preferred"
                                            NetworkModeOption.MODE_NR_ONLY -> "5G Only (NR)"
                                            NetworkModeOption.MODE_LTE_ONLY -> "4G Only (LTE)"
                                            NetworkModeOption.MODE_3G_ONLY -> "3G Only"
                                            NetworkModeOption.MODE_2G_ONLY -> "2G Only"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (showDialog) {
                            NetworkModeSelectionDialog(
                                currentMode = currentMode,
                                onDismiss = { showDialog = false },
                                onSelect = { mode ->
                                    currentSelectedModes[sim.id] = mode
                                    setAllowedNetworkTypes(context, sim.id, sim.slotIndex, mode)
                                    showDialog = false
                                    HapticUtil.performUIHaptic(view)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NetworkModeSelectionDialog(
    currentMode: NetworkModeOption,
    onDismiss: () -> Unit,
    onSelect: (NetworkModeOption) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Network Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NetworkModeDialogItem(
                    label = "5G Preferred (All bands)",
                    isSelected = currentMode == NetworkModeOption.MODE_5G_PREF,
                    onClick = { onSelect(NetworkModeOption.MODE_5G_PREF) }
                )
                NetworkModeDialogItem(
                    label = "5G Only (NR Only)",
                    isSelected = currentMode == NetworkModeOption.MODE_NR_ONLY,
                    onClick = { onSelect(NetworkModeOption.MODE_NR_ONLY) }
                )
                NetworkModeDialogItem(
                    label = "4G Only (LTE Only)",
                    isSelected = currentMode == NetworkModeOption.MODE_LTE_ONLY,
                    onClick = { onSelect(NetworkModeOption.MODE_LTE_ONLY) }
                )
                NetworkModeDialogItem(
                    label = "3G Only",
                    isSelected = currentMode == NetworkModeOption.MODE_3G_ONLY,
                    onClick = { onSelect(NetworkModeOption.MODE_3G_ONLY) }
                )
                NetworkModeDialogItem(
                    label = "2G Only",
                    isSelected = currentMode == NetworkModeOption.MODE_2G_ONLY,
                    onClick = { onSelect(NetworkModeOption.MODE_2G_ONLY) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun NetworkModeDialogItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun switchDefaultDataSubId(context: Context, subId: Int) {
    try {
        val binder = SystemServiceHelper.getSystemService("isub")
        if (binder != null) {
            val wrappedBinder = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("com.android.internal.telephony.ISub\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val iSubInstance = asInterfaceMethod.invoke(null, wrappedBinder)
            
            val setDefaultDataSubIdMethod = iSubInstance.javaClass.getMethod(
                "setDefaultDataSubId",
                Int::class.javaPrimitiveType
            )
            setDefaultDataSubIdMethod.invoke(iSubInstance, subId)
            Log.d("NetworkModeSettings", "Successfully set default data subscription to $subId via UI")
            
            // Auto enable mobile data on target subId
            enableMobileData(context, subId)
        }
    } catch (e: Exception) {
        Log.e("NetworkModeSettings", "Error switching default data SIM via UI", e)
    }
}

private fun enableMobileData(context: Context, subId: Int) {
    try {
        val binder = SystemServiceHelper.getSystemService("phone")
        if (binder != null) {
            val wrappedBinder = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val iTelephonyInstance = asInterfaceMethod.invoke(null, wrappedBinder)
            
            var setDataEnabledMethod: java.lang.reflect.Method? = null
            var args = arrayOf<Any>()

            // Try setDataEnabledForReason(int subId, int reason, boolean enable, String callingPackage) - Android 14+
            try {
                setDataEnabledMethod = iTelephonyInstance.javaClass.getMethod(
                    "setDataEnabledForReason",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    String::class.java
                )
                args = arrayOf(subId, 0, true, context.packageName)
            } catch (e: NoSuchMethodException) {
                // Try setDataEnabled(int subId, boolean enable) - older versions
                try {
                    setDataEnabledMethod = iTelephonyInstance.javaClass.getMethod(
                        "setDataEnabled",
                        Int::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType
                    )
                    args = arrayOf(subId, true)
                } catch (ex: NoSuchMethodException) {
                    Log.e("NetworkModeSettings", "Could not find setDataEnabled or setDataEnabledForReason method", ex)
                }
            }

            if (setDataEnabledMethod != null) {
                setDataEnabledMethod.invoke(iTelephonyInstance, *args)
                Log.d("NetworkModeSettings", "Successfully enabled mobile data for subId=$subId")
            }
        }
    } catch (e: Exception) {
        Log.e("NetworkModeSettings", "Error enabling mobile data for subId=$subId", e)
    }
}



private fun getDefaultDataSubIdFromShell(context: Context): Int {
    val output = ShellUtils.runCommandWithOutput(context, "settings get global multi_sim_data_call")
    return output?.trim()?.toIntOrNull() ?: -1
}

private fun getCurrentNetworkMode(context: Context, subId: Int, slotId: Int): NetworkModeOption {
    var currentAllowed: Long = -1L
    
    try {
        val binder = SystemServiceHelper.getSystemService("phone")
        if (binder != null) {
            val wrappedBinder = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val iTelephonyInstance = asInterfaceMethod.invoke(null, wrappedBinder)
            
            val getMethod = iTelephonyInstance.javaClass.getMethod(
                "getAllowedNetworkTypesForReason",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            currentAllowed = getMethod.invoke(iTelephonyInstance, subId, 0) as Long
        }
    } catch (e: Exception) {
        Log.e("NetworkModeSettings", "Error getting allowed network types via standard reflection", e)
    }

    if (currentAllowed == -1L) {
        val output = ShellUtils.runCommandWithOutput(context, "cmd phone get-allowed-network-types-for-users -s $slotId")
        if (output.isNullOrBlank()) {
            return NetworkModeOption.MODE_5G_PREF
        }
        val hasNR = output.contains("NR", ignoreCase = true)
        val hasLTE = output.contains("LTE", ignoreCase = true)
        val has3G = output.contains("UMTS", ignoreCase = true) || output.contains("HSPA", ignoreCase = true) || output.contains("HSDPA", ignoreCase = true)
        val has2G = output.contains("GSM", ignoreCase = true) || output.contains("GPRS", ignoreCase = true) || output.contains("EDGE", ignoreCase = true)

        return when {
            hasNR && !hasLTE && !has3G && !has2G -> NetworkModeOption.MODE_NR_ONLY
            !hasNR && hasLTE && !has3G && !has2G -> NetworkModeOption.MODE_LTE_ONLY
            !hasNR && !hasLTE && has3G && !has2G -> NetworkModeOption.MODE_3G_ONLY
            !hasNR && !hasLTE && !has3G && has2G -> NetworkModeOption.MODE_2G_ONLY
            else -> NetworkModeOption.MODE_5G_PREF
        }
    }

    val bitmask2G = getBitmaskForTech(16) or getBitmaskForTech(1) or getBitmaskForTech(2) or getBitmaskForTech(4) or getBitmaskForTech(7) or getBitmaskForTech(11)
    val bitmask3G = getBitmaskForTech(3) or getBitmaskForTech(5) or getBitmaskForTech(6) or getBitmaskForTech(12) or getBitmaskForTech(8) or getBitmaskForTech(9) or getBitmaskForTech(10) or getBitmaskForTech(14) or getBitmaskForTech(15)
    val bitmask4G = getBitmaskForTech(13) or getBitmaskForTech(19)
    val bitmask5G = getBitmaskForTech(20)

    val hasNR = (currentAllowed and bitmask5G) != 0L
    val hasLTE = (currentAllowed and bitmask4G) != 0L
    val has3G = (currentAllowed and bitmask3G) != 0L
    val has2G = (currentAllowed and bitmask2G) != 0L

    return when {
        hasNR && !hasLTE && !has3G && !has2G -> NetworkModeOption.MODE_NR_ONLY
        !hasNR && hasLTE && !has3G && !has2G -> NetworkModeOption.MODE_LTE_ONLY
        !hasNR && !hasLTE && has3G && !has2G -> NetworkModeOption.MODE_3G_ONLY
        !hasNR && !hasLTE && !has3G && has2G -> NetworkModeOption.MODE_2G_ONLY
        else -> NetworkModeOption.MODE_5G_PREF
    }
}

private fun setAllowedNetworkTypes(context: Context, subId: Int, slotId: Int, mode: NetworkModeOption) {
    val bitmask = when (mode) {
        NetworkModeOption.MODE_5G_PREF -> {
            val bit2G = getBitmaskForTech(16) or getBitmaskForTech(1) or getBitmaskForTech(2) or getBitmaskForTech(4) or getBitmaskForTech(7) or getBitmaskForTech(11)
            val bit3G = bit2G or getBitmaskForTech(3) or getBitmaskForTech(5) or getBitmaskForTech(6) or getBitmaskForTech(12) or getBitmaskForTech(8) or getBitmaskForTech(9) or getBitmaskForTech(10) or getBitmaskForTech(14) or getBitmaskForTech(15)
            val bit4G = getBitmaskForTech(13) or getBitmaskForTech(19)
            val bit5G = getBitmaskForTech(20)
            bit2G or bit3G or bit4G or bit5G
        }
        NetworkModeOption.MODE_NR_ONLY -> getBitmaskForTech(20)
        NetworkModeOption.MODE_LTE_ONLY -> getBitmaskForTech(13) or getBitmaskForTech(19)
        NetworkModeOption.MODE_3G_ONLY -> {
            val bit2G = getBitmaskForTech(16) or getBitmaskForTech(1) or getBitmaskForTech(2) or getBitmaskForTech(4) or getBitmaskForTech(7) or getBitmaskForTech(11)
            bit2G or getBitmaskForTech(3) or getBitmaskForTech(5) or getBitmaskForTech(6) or getBitmaskForTech(12) or getBitmaskForTech(8) or getBitmaskForTech(9) or getBitmaskForTech(10) or getBitmaskForTech(14) or getBitmaskForTech(15)
        }
        NetworkModeOption.MODE_2G_ONLY -> getBitmaskForTech(16) or getBitmaskForTech(1) or getBitmaskForTech(2) or getBitmaskForTech(4) or getBitmaskForTech(7) or getBitmaskForTech(11)
    }

    try {
        val binder = SystemServiceHelper.getSystemService("phone")
        if (binder != null) {
            val wrappedBinder = ShizukuBinderWrapper(binder)
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val iTelephonyInstance = asInterfaceMethod.invoke(null, wrappedBinder)
            
            var setMethod: java.lang.reflect.Method? = null
            var args = arrayOf<Any>()

            // Try 4-argument signature (Android 14/15/16/17+)
            try {
                setMethod = iTelephonyInstance.javaClass.getMethod(
                    "setAllowedNetworkTypesForReason",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType,
                    String::class.java
                )
                args = arrayOf(subId, 0, bitmask, context.packageName)
            } catch (e: NoSuchMethodException) {
                // Fall back to 3-argument signature (Android 11/12/13)
                try {
                    setMethod = iTelephonyInstance.javaClass.getMethod(
                        "setAllowedNetworkTypesForReason",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Long::class.javaPrimitiveType
                    )
                    args = arrayOf(subId, 0, bitmask)
                } catch (ex: NoSuchMethodException) {
                    Log.e("NetworkModeSettings", "Both 4-argument and 3-argument setAllowedNetworkTypesForReason methods not found", ex)
                }
            }

            if (setMethod != null) {
                setMethod.invoke(iTelephonyInstance, *args)
                Log.d("NetworkModeSettings", "Successfully set allowed network types to $bitmask via standard reflection")
                return
            }
        }
    } catch (e: Exception) {
        Log.e("NetworkModeSettings", "Error setting allowed network types via standard reflection", e)
    }

    // Fallback to shell
    val bitmaskString = longToBinaryString20(bitmask)
    ShellUtils.runCommand(context, "cmd phone set-allowed-network-types-for-users -s $slotId $bitmaskString")
}

private fun getBitmaskForTech(tech: Int): Long {
    return if (tech > 0) (1L shl (tech - 1)) else 0L
}

private fun longToBinaryString20(mask: Long): String {
    val sb = java.lang.StringBuilder()
    for (i in 20 downTo 1) {
        val bit = if ((mask and (1L shl (i - 1))) != 0L) '1' else '0'
        sb.append(bit)
    }
    return sb.toString()
}



enum class NetworkModeOption {
    MODE_5G_PREF, MODE_NR_ONLY, MODE_LTE_ONLY, MODE_3G_ONLY, MODE_2G_ONLY
}
