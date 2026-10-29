package com.sameerasw.essentials.ui.features.display.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.features.system.RemapActionItem

val HORIZONTAL_SLIDE_MODES = listOf(
    Triple("none", R.string.duo_action_horizontal_slide_none, R.drawable.rounded_do_not_disturb_on_24),
    Triple("volume", R.string.duo_action_horizontal_slide_volume, R.drawable.rounded_volume_up_24),
    Triple("brightness", R.string.duo_action_horizontal_slide_brightness, R.drawable.rounded_brightness_6_24),
    Triple("sound_mode", R.string.duo_action_horizontal_slide_sound_mode, R.drawable.rounded_mobile_sound_24),
)

@Composable
fun horizontalSlideDescription(mode: String, track: Boolean): String {
    val base = stringResource(HORIZONTAL_SLIDE_MODES.firstOrNull { it.first == mode }?.second ?: R.string.duo_action_horizontal_slide_none)
    val trackLabel = stringResource(R.string.duo_action_horizontal_slide_track)
    return when {
        !track -> base
        mode != "none" -> "$base • $trackLabel"
        else -> trackLabel
    }
}

@Composable
fun HorizontalSlideModeSheet(
    mode: String,
    onModeSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.duo_action_horizontal_slide_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RoundedCardContainer(spacing = 2.dp) {
                HORIZONTAL_SLIDE_MODES.forEach { (modeKey, titleRes, iconRes) ->
                    RemapActionItem(
                        title = stringResource(titleRes),
                        iconRes = iconRes,
                        isSelected = mode == modeKey,
                        onClick = {
                            onModeSelected(modeKey)
                            if (extraContent == null) onDismissRequest()
                        },
                    )
                }
            }

            extraContent?.invoke()
        }
    }
}
