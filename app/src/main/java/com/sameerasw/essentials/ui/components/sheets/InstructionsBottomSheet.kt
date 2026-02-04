package com.sameerasw.essentials.ui.components.sheets

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

data class InstructionStep(
    val instruction: String,
    val imageRes: Int
)

data class InstructionSection(
    val title: String,
    val iconRes: Int,
    val description: String? = null,
    val steps: List<InstructionStep> = emptyList(),
    val links: List<Pair<String, String>> = emptyList() // Pair(label, url)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sections = listOf(
        InstructionSection(
            title = stringResource(R.string.instruction_section_perms_title),
            iconRes = R.drawable.rounded_security_24,
            steps = listOf(
                InstructionStep(
                    instruction = stringResource(R.string.instruction_step_perms_1),
                    imageRes = R.drawable.accessibility_1
                ),
                InstructionStep(
                    instruction = stringResource(R.string.instruction_step_perms_2),
                    imageRes = R.drawable.accessibility_2
                ),
                InstructionStep(
                    instruction = stringResource(R.string.instruction_step_perms_3),
                    imageRes = R.drawable.accessibility_3
                )
            )
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_shizuku_title),
            iconRes = R.drawable.rounded_adb_24,
            description = stringResource(R.string.instruction_section_shizuku_desc),
            links = listOf(
                stringResource(R.string.label_shizuku_ritaka) to "https://github.com/RikkaApps/Shizuku",
                stringResource(R.string.label_shizuku_tuozi) to "https://github.com/yangFenTuoZi/Shizuku"
            )
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_maps_title),
            iconRes = R.drawable.rounded_navigation_24,
            description = stringResource(R.string.instruction_section_maps_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_silent_title),
            iconRes = R.drawable.rounded_volume_off_24,
            description = stringResource(R.string.instruction_section_silent_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_freeze_title),
            iconRes = R.drawable.rounded_mode_cool_24,
            description = stringResource(R.string.instruction_section_freeze_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_security_title),
            iconRes = R.drawable.rounded_security_24,
            description = stringResource(R.string.instruction_section_security_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_statusbar_title),
            iconRes = R.drawable.rounded_interests_24,
            description = stringResource(R.string.instruction_section_statusbar_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_lighting_title),
            iconRes = R.drawable.rounded_blur_linear_24,
            description = stringResource(R.string.instruction_section_lighting_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_button_title),
            iconRes = R.drawable.rounded_switch_access_3_24,
            description = stringResource(R.string.instruction_section_button_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_flashlight_title),
            iconRes = R.drawable.rounded_flashlight_on_24,
            description = stringResource(R.string.instruction_section_flashlight_desc)
        ),
        InstructionSection(
            title = stringResource(R.string.instruction_section_about_title),
            iconRes = R.drawable.ic_stat_name,
            description = stringResource(R.string.instruction_section_about_desc)
        )
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.help_guides_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                )
            }

            item {
                RoundedCardContainer {
                    sections.forEach { section ->
                        ExpandableGuideSection(section)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.need_more_support_reach_out),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 3
                ) {
                    Button(
                        onClick = {
                            val websiteUrl = "https://github.com/sameerasw/essentials"
                            val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.brand_github),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_view_on_github))
                    }

                    OutlinedButton(
                        onClick = {
                            val mailUri = "mailto:mail@sameerasw.com".toUri()
                            val emailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                                putExtra(Intent.EXTRA_SUBJECT, "Hello from Essentials")
                            }
                            try {
                                context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.send_email_chooser_title)))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, context.getString(R.string.error_no_email_app), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mail_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.email_label))
                    }

                    OutlinedButton(
                        onClick = {
                            val websiteUrl = "https://t.me/tidwib"
                            val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.brand_telegram),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.support_group_label))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableGuideSection(section: InstructionSection) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrow_rotation")
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = section.iconRes),
                            contentDescription = null,
                            tint = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                    contentDescription = if (expanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                    modifier = Modifier.rotate(rotation)
                )
            }

            // Content
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (section.description != null) {
                        Text(
                            text = section.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f)
                        )
                    }

                    if (section.steps.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            section.steps.forEachIndexed { index, step ->
                                InstructionStepItem(
                                    stepNumber = index + 1,
                                    instruction = step.instruction,
                                    imageRes = step.imageRes
                                )
                            }
                        }
                    }

                    if (section.links.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            section.links.forEach { (label, url) ->
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.brand_github),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionStepItem(
    stepNumber: Int,
    instruction: String,
    imageRes: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = instruction,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = stringResource(R.string.instruction_step_image_description, stepNumber),
            modifier = Modifier
                .fillMaxWidth(fraction = 0.95f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth
        )
    }
}
