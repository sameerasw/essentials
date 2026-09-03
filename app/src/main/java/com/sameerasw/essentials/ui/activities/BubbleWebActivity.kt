/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module - Native Bubble Web Preview
 * File: BubbleWebActivity.kt
 * Description: Dedicated resizable bubble activity for private web previews using native Android Bubbles API.
 */

package com.sameerasw.essentials.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.LinkPickerActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlin.math.roundToInt

class BubbleWebActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_bubble_url"
        const val EXTRA_PRIVATE_MODE = "extra_bubble_private_mode"
        const val EXTRA_FULLSCREEN = "extra_bubble_fullscreen"
    }

    private var webViewInstance: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialUrl = intent.getStringExtra(EXTRA_URL) ?: intent.dataString ?: "https://google.com"
        val isPrivateMode = intent.getBooleanExtra(EXTRA_PRIVATE_MODE, true)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                BubbleWebScreen(
                    initialUrl = initialUrl,
                    isPrivate = isPrivateMode,
                    onClose = { finish() },
                    onAttachWebView = { webViewInstance = it },
                )
            }
        }
    }

    override fun onDestroy() {
        try {
            webViewInstance?.clearCache(true)
            webViewInstance?.clearHistory()
            webViewInstance?.clearFormData()
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)
            webViewInstance?.destroy()
            webViewInstance = null
        } catch (_: Exception) {}
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BubbleWebScreen(
    initialUrl: String,
    isPrivate: Boolean,
    onClose: () -> Unit,
    onAttachWebView: (WebView) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    val repository = remember { SettingsRepository(context) }
    var isTopGapEnabled by remember { mutableStateOf(!repository.isBubbleWebFullscreen()) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val animatedTopPadding by animateDpAsState(
        targetValue = if (isTopGapEnabled) statusBarTop else 0.dp,
        label = "TopGapPadding",
    )
    val animatedTopCornerRadius by animateDpAsState(
        targetValue = if (isTopGapEnabled) 24.dp else 0.dp,
        label = "TopCornerRadius",
    )

    val toolbarMaxOffsetPx = with(density) { 160.dp.toPx() }
    var toolbarOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                toolbarOffsetPx = (toolbarOffsetPx - delta).coerceIn(0f, toolbarMaxOffsetPx)
                return Offset.Zero
            }
        }
    }

    BackHandler(enabled = true) {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onClose()
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = animatedTopPadding)
                    .clip(
                        RoundedCornerShape(
                            topStart = animatedTopCornerRadius,
                            topEnd = animatedTopCornerRadius,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp,
                        )
                    ),
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = !isPrivate
                                cacheMode = if (isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }

                            if (isPrivate) {
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                            }

                            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                                val delta = (scrollY - oldScrollY).toFloat()
                                toolbarOffsetPx = (toolbarOffsetPx + delta).coerceIn(0f, toolbarMaxOffsetPx)
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    pageProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                    canGoBack = view?.canGoBack() == true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    currentUrl = url
                                    canGoBack = view?.canGoBack() == true
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                    canGoBack = view?.canGoBack() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    canGoBack = view?.canGoBack() == true
                                }
                            }

                            loadUrl(currentUrl)
                            webViewRef = this
                            onAttachWebView(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val toolbarVisibilityRatio = 1f - (toolbarOffsetPx / toolbarMaxOffsetPx).coerceIn(0f, 1f)
            val currentDomain = remember(currentUrl) { extractDomain(currentUrl) }
            val copyFeedbackText = stringResource(R.string.bubble_web_link_copied)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = navBarBottom + 16.dp)
                    .offset { IntOffset(0, toolbarOffsetPx.roundToInt()) }
                    .alpha(toolbarVisibilityRatio),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                        toolbarContainerColor = MaterialTheme.colorScheme.primary,
                        toolbarContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.height(72.dp),
                ) {
                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            if (webViewRef?.canGoBack() == true) {
                                webViewRef?.goBack()
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (canGoBack) R.drawable.rounded_arrow_back_24 else R.drawable.rounded_close_24
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .combinedClickable(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                                    Toast.makeText(context, copyFeedbackText, Toast.LENGTH_SHORT).show()
                                },
                                onLongClick = {
                                    HapticUtil.performHeavyHaptic(view)
                                    val newGapState = !isTopGapEnabled
                                    isTopGapEnabled = newGapState
                                    repository.setBubbleWebFullscreen(!newGapState)
                                },
                            )
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading && pageProgress < 1f) {
                            LinearWavyProgressIndicator(
                                progress = { pageProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f),
                            )
                        } else {
                            Text(
                                text = currentDomain,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                            Toast.makeText(context, copyFeedbackText, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_link_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val shareIntent = Intent(context, LinkPickerActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse(currentUrl)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_share_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun extractDomain(url: String): String {
    return try {
        val uri = Uri.parse(url)
        val host = uri.host ?: url
        if (host.startsWith("www.")) host.substring(4) else host
    } catch (_: Exception) {
        url
    }
}
