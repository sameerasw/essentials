package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationIconPicker(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = listOf(
        "round_navigation_24",
        "rounded_home_24",
        "rounded_work_24",
        "rounded_apartment_24",
        "rounded_shopping_cart_24",
        "rounded_school_24",
        "rounded_storefront_24",
        "rounded_fork_spoon_24",
        "rounded_favorite_24",
        "rounded_account_balance_24",
        "rounded_garage_home_24",
        "rounded_beach_access_24",
        "rounded_local_pizza_24",
        "rounded_train_24",
        "rounded_directions_bus_24",
        "rounded_flight_24",
        "rounded_directions_boat_24"
    )

    val carouselState = rememberCarouselState { icons.size }
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Pick an icon",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 64.dp,
            itemSpacing = 4.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) { index ->
            val iconName = icons[index]
            val isSelected = iconName == selectedIconName
            val iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.background
                    )
                    .clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onIconSelected(iconName)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (iconResId != 0) iconResId else R.drawable.round_navigation_24),
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
