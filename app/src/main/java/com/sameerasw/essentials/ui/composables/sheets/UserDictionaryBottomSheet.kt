package com.sameerasw.essentials.ui.composables.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDictionaryBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Load words on open
    LaunchedEffect(Unit) {
        viewModel.loadUserDictionaryWords(context)
    }

    val words = viewModel.userDictionaryWords.value

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Learned Words",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (words.isEmpty()) {
                Text(
                    text = "No words learned yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(words.entries.toList().sortedByDescending { it.value }) { (word, freq) ->
                         RoundedCardContainer(
                             spacing = 0.dp,
                             containerColor = MaterialTheme.colorScheme.surfaceBright
                         ) {
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(horizontal = 16.dp, vertical = 12.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(
                                         text = word,
                                         style = MaterialTheme.typography.titleMedium
                                     )
                                 }
                                 
                                 IconButton(
                                     onClick = { viewModel.deleteUserWord(word, context) }
                                 ) {
                                     Icon(
                                         painter = painterResource(R.drawable.rounded_delete_24),
                                         contentDescription = "Delete",
                                         tint = MaterialTheme.colorScheme.error
                                     )
                                 }
                             }
                         }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
