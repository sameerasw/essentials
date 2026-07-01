package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

class PixelSearchbarSettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = LocalContext.current

            remember(context) { viewModel.check(context) }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val statusBarHeight =
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(statusBarHeight))

                        PixelSearchbarSettingsUI(
                            viewModel = viewModel,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(150.dp))
                    }

                    EssentialsFloatingToolbar(
                        title = stringResource(R.string.pixel_searchbar_settings_title),
                        onBackClick = { finish() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixelSearchbarSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentType = viewModel.pixelSearchbarType.value

    val options = listOf("empty", "date")
    val labels = mapOf(
        "empty" to stringResource(R.string.pixel_searchbar_style_empty),
        "date" to stringResource(R.string.pixel_searchbar_style_date)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.pixel_searchbar_settings_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 0.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ListItem(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_home_24),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    content = {
                        Text(
                            text = stringResource(R.string.feat_pixel_searchbar_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    options.forEachIndexed { index, type ->
                        val isChecked = currentType == type
                        val label = labels[type] ?: type

                        ToggleButton(
                            checked = isChecked,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setPixelSearchbarType(type, context)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when {
                                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
