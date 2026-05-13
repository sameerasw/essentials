package com.sameerasw.essentials.ui.components.sheets

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.HelpAndGuidesContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
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
                HelpAndGuidesContent()
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
                                context.startActivity(
                                    Intent.createChooser(
                                        emailIntent,
                                        context.getString(R.string.send_email_chooser_title)
                                    )
                                )
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_no_email_app),
                                    Toast.LENGTH_SHORT
                                ).show()
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
