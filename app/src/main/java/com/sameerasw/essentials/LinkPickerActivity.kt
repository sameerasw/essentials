package com.sameerasw.essentials

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sameerasw.essentials.ui.components.linkActions.LinkPickerScreen
import com.sameerasw.essentials.ui.theme.EssentialsTheme

class LinkPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationViewModel =
            com.sameerasw.essentials.viewmodels.LocationReachedViewModel(application)
        if (locationViewModel.handleIntent(intent)) {
            val settingsIntent = Intent(this, FeatureSettingsActivity::class.java).apply {
                putExtra("feature", "Location reached")
            }
            startActivity(settingsIntent)
            finish()
            return
        }

        val uri = when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                extractUrl(text)?.let { Uri.parse(it) }
            }

            else -> intent.data
        }

        if (uri == null) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                LinkPickerScreen(
                    uri = uri,
                    onFinish = { finish() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun extractUrl(text: String): String? {
    val urlRegex = Regex("https?://[\\w\\.-]+(?:\\:[0-9]+)?(?:/[^\\s]*)?")
    return urlRegex.find(text)?.value
}
