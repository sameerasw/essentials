/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: ContributorsCarousel.kt
 * Description: Material 3 Carousel displaying repository contributors.
 */

package com.sameerasw.essentials.ui.components.dialogs

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.ContributorsRepository
import com.sameerasw.essentials.domain.model.github.GitHubContributor
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributorsCarousel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    var contributors by remember { mutableStateOf<List<GitHubContributor>>(emptyList()) }

    LaunchedEffect(Unit) {
        val repo = ContributorsRepository.getInstance(context)
        contributors = repo.getContributors()
    }

    AnimatedVisibility(
        visible = contributors.isNotEmpty(),
        enter =
            expandVertically(
                animationSpec =
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioNoBouncy,
                    ),
            ) + fadeIn(animationSpec = tween(500)),
        exit =
            shrinkVertically(
                animationSpec =
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioNoBouncy,
                    ),
            ) + fadeOut(animationSpec = tween(300)),
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TranslatableText(
                    stringResId = R.string.label_top_contributors,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }

            val carouselState = rememberCarouselState { contributors.size }

            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = 110.dp,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(135.dp),
            ) { index ->
                val contributor = contributors[index]

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .maskClip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                HapticUtil.performUIHaptic(view)
                                val intent = Intent(Intent.ACTION_VIEW, contributor.htmlUrl.toUri())
                                context.startActivity(intent)
                            },
                ) {
                    AsyncImage(
                        model = contributor.avatarUrl,
                        contentDescription = contributor.login,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (contributor.contributions > 0) {
                        Surface(
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_commit_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "${contributor.contributions}",
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = GoogleSansFlexRounded,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Black.copy(alpha = 0.65f),
                                            ),
                                    ),
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Text(
                            text = contributor.login,
                            style =
                                MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontWeight = FontWeight.Normal,
                                ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
