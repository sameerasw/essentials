package com.sameerasw.essentials

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.sameerasw.essentials.ui.LinkPickerScreen
import com.sameerasw.essentials.ui.theme.EssentialsTheme

class LinkPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            EssentialsTheme {
                LinkPickerScreen(uri = uri, onFinish = { finish() }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun extractUrl(text: String): String? {
    val urlRegex = Regex("https?://[\\w\\.-]+(?:\\:[0-9]+)?(?:/[^\\s]*)?")
    return urlRegex.find(text)?.value
}
