package com.sameerasw.essentials.island.plugins.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NavigationNotificationData
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandLayoutSpec
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.islandTextColor
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.ui.theme.EssentialsTheme

@Composable
fun NavigationExpanded(
    data: NavigationNotificationData,
    accent: Color,
    onOpen: () -> Unit,
    onExit: (() -> Unit)?,
    scope: IslandExpandedScope,
) {
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    Box(propagateMinConstraints = true) {
        Column(Modifier.fillMaxWidth().padding(spec.expandedOutset)) {
            Spacer(Modifier.height(spec.expandedTopPadding))
            scope.CameraRow(
                horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                start = { Maneuver(data, spec.cellSize.value.toInt()) },
                end = {
                    data.distance?.let {
                        Text(it, style = IslandTextStyles.title.copy(color = accent), maxLines = 1)
                    }
                },
            )
            if (!data.instruction.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = data.instruction,
                    style = IslandTextStyles.title.copy(color = islandTextColor(), fontSize = 20.sp, lineHeight = 25.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                )
            }
            if (!data.eta.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = data.eta,
                    style = IslandTextStyles.body.copy(color = islandTextColor(0.7f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                )
            }
            Spacer(Modifier.height(16.dp))
            ConnectedButtonRow(
                height = 44.dp,
                modifier = Modifier.padding(horizontal = sidePadding).padding(bottom = spec.expandedBottomPadding),
                container = Color.White.copy(alpha = 0.2f),
                items = listOfNotNull(
                    ConnectedItem({
                        onOpen()
                        scope.collapse()
                    }) { ConnectedTextLabel(stringResource(R.string.island_progress_open)) },
                    onExit?.let { exit ->
                        ConnectedItem({
                            exit()
                            scope.collapse()
                        }) { ConnectedTextLabel(stringResource(R.string.island_navigation_exit)) }
                    },
                ),
            )
        }
    }
}
