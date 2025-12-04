package com.sameerasw.essentials.ui.components.dialogs

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R

@Composable
fun AboutSection(
    modifier: Modifier = Modifier,
    appName: String = "Essentials",
    developerName: String = "Sameera Wijerathna",
    description: String = "The all-in-one toolbox for your Pixel"
) {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (_: Exception) {
        "Unknown"
    }

    Surface(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "$appName v$versionName", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(id = R.drawable.avatar),
                contentDescription = "Developer Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Developed by $developerName\nwith ❤\uFE0F from \uD83C\uDDF1\uD83C\uDDF0", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                OutlinedButton(onClick = {
                    // Use mailto: URI so the system opens an email client
                    val mailUri = "mailto:mail@sameerasw.com".toUri()
                    val emailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "Hello from Essentials")
                    }
                    try {
                        context.startActivity(Intent.createChooser(emailIntent, "Send email"))
                    } catch (e: android.content.ActivityNotFoundException) {
                        Log.w("AboutSection", "No email app available", e)
                        Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Contact me")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    val websiteUrl = "https://sameerasw.com"
                    val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                    context.startActivity(intent)
                }) {
                    Text("My website")
                }
            }
        }
    }
}
