package com.sameerasw.essentials.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme

class AutomationEditorActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_AUTOMATION_ID = "automation_id"
        private const val EXTRA_AUTOMATION_TYPE = "automation_type"

        fun createIntent(context: Context, automationId: String): Intent {
            return Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_ID, automationId)
            }
        }

        fun createIntent(context: Context, type: Automation.Type): Intent {
            return Intent(context, AutomationEditorActivity::class.java).apply {
                putExtra(EXTRA_AUTOMATION_TYPE, type.name)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID)
        val automationTypeStr = intent.getStringExtra(EXTRA_AUTOMATION_TYPE)
        
        // title logic
        val isEditMode = automationId != null
        val titleRes = if (isEditMode) R.string.diy_editor_edit_title else R.string.diy_editor_new_title

        setContent {
            EssentialsTheme {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = titleRes,
                            hasBack = true,
                            onBackClick = { finish() }
                        )
                    }
                ) { innerPadding ->
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val carouselState = rememberCarouselState { 2 } // 0: Trigger/State, 1: Actions

                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = screenWidth,
                        itemSpacing = 4.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .maskClip(MaterialTheme.shapes.extraLarge)
                        ) {
                            if (index == 0) {
                                // Trigger / State Editor
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                                    Text("Trigger/State Editor")
                                }
                            } else {
                                // Actions Editor
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceBright), contentAlignment = Alignment.Center) {
                                    Text("Actions Editor")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
