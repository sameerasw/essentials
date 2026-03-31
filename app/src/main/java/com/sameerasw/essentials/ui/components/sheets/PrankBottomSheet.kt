package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrankBottomSheet(
    viewModel: MainViewModel,
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = {
            if (isRevealed) {
                onDismissRequest()
            }
        },
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo
            AsyncImage(
                model = R.mipmap.ic_launcher_round,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )

            if (!isRevealed) {
                // Prank Phase
                Text(
                    text = stringResource(R.string.prank_trial_expired_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.prank_trial_expired_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        HapticUtil.performHeavyHaptic(view)
                        isRevealed = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.prank_button_premium))
                }
            } else {
                // Reveal Phase
                Text(
                    text = stringResource(R.string.prank_reveal_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.prank_reveal_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    // Debug reset
                                    viewModel.settingsRepository.putBoolean(
                                        SettingsRepository.KEY_APRIL_FOOLS_SHOWN,
                                        false
                                    )
                                    HapticUtil.performHeavyHaptic(view)
                                    android.widget.Toast.makeText(view.context, "Prank reset for testing", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onTap = {
                                    HapticUtil.performUIHaptic(view)
                                    onDismissRequest()
                                }
                            )
                        }
                ) {
                    Text(text = stringResource(R.string.action_continue))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
