package com.sameerasw.essentials.ui.components.watermark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.viewmodels.WatermarkUiState

@Composable
fun WatermarkPreview(
    uiState: WatermarkUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is WatermarkUiState.Idle -> {
                Text(
                    text = stringResource(R.string.watermark_pick_image),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is WatermarkUiState.Processing -> {
                CircularProgressIndicator()
            }
            is WatermarkUiState.Success -> {
                val targetFile = uiState.file
                var visibleFile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(targetFile) }
                var targetReady by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     AsyncImage(
                        model = visibleFile,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                     )
                     
                     if (targetFile != visibleFile) {
                         AsyncImage(
                            model = targetFile,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onSuccess = {
                                visibleFile = targetFile
                            }
                         )
                     }
                }
            }
            is WatermarkUiState.Error -> {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
