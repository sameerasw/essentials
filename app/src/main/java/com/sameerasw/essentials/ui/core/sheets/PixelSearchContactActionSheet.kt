/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Sheets
 * File: PixelSearchContactActionSheet.kt
 * Description: Contextual bottom sheet providing contact actions (Call, SMS, View contact, Copy number).
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.PixelSearchResultItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun PixelSearchContactActionSheet(
    contactItem: PixelSearchResultItem.ContactItem,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                if (contactItem.photoUri != null) {
                    AsyncImage(
                        model = contactItem.photoUri,
                        contentDescription = contactItem.name,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(52.dp)
                                .background(
                                    color = ColorUtil.iconContainerColor(contactItem.name),
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_call_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    contactItem.phoneNumber?.let { num ->
                        Text(
                            text = num,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                if (contactItem.phoneNumber != null) {
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_call),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val callIntent =
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contactItem.phoneNumber}"))
                            context.startActivity(callIntent)
                            onDismissRequest()
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_call_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_sms),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val smsIntent =
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contactItem.phoneNumber}"))
                            context.startActivity(smsIntent)
                            onDismissRequest()
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_chat_bubble_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_copy_phone),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Phone Number", contactItem.phoneNumber)
                            clipboard.setPrimaryClip(clip)
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                Toast.makeText(context, context.getString(R.string.pixel_search_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                            }
                            onDismissRequest()
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_content_copy_24,
                        hasIconBackground = true,
                    )
                }
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_view_contact),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        val viewIntent =
                            Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactItem.id))
                        context.startActivity(viewIntent)
                        onDismissRequest()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_person_24,
                    hasIconBackground = true,
                )
            }
        }
    }
}
