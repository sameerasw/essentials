package com.sameerasw.essentials

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.composables.ScreenOffWidgetSetup
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // initialize permission registry
        initPermissionRegistry()
        viewModel.check(this)
        setContent {
            EssentialsTheme {
                var searchRequested by remember { mutableStateOf(false) }
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = "Essentials",
                            hasBack = false,
                            hasSearch = true,
                            hasSettings = true,
                            onSearchClick = { searchRequested = true },
                            onSettingsClick = { startActivity(Intent(this, SettingsActivity::class.java)) },
                            scrollBehavior = scrollBehavior,
                            subtitle = "V1.0"
                        )
                    }
                ) { innerPadding ->
                    ScreenOffWidgetSetup(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        searchRequested = searchRequested,
                        onSearchHandled = { searchRequested = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.check(this)
    }
}
