/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Modifiers
 * File: ConsumeHorizontalScrollModifier.kt
 * Description: Stops leftover horizontal scroll from reaching parent pagers.
 */

package com.sameerasw.essentials.ui.modifiers

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

fun Modifier.consumeHorizontalScroll(): Modifier =
    composed {
        val connection =
            remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset = Offset(x = available.x, y = 0f)

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity,
                    ): Velocity = Velocity(x = available.x, y = 0f)
                }
            }
        nestedScroll(connection)
    }
