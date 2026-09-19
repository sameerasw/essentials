/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: TestNotificationUtil.kt
 * Description: Realistic test notification generator for Dynamic Island preview with rich apps and actions.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.domain.model.NotificationActionItem
import kotlin.random.Random

object TestNotificationUtil {

    private data class AppPreset(
        val appName: String,
        val packageName: String,
        val color: Int,
        val iconLetter: String,
        val sampleSenders: List<String>,
        val sampleTitles: List<String>,
        val sampleBodies: List<String>,
        val sampleActions: List<List<NotificationActionItem>>
    )

    private val apps = listOf(
        AppPreset(
            appName = "WhatsApp",
            packageName = "com.whatsapp",
            color = 0xFF25D366.toInt(),
            iconLetter = "W",
            sampleSenders = listOf("Alice Johnson", "Michael Chen", "Sarah Williams", "Family Group", "Team Alpha"),
            sampleTitles = listOf("Alice Johnson", "Michael Chen", "Sarah Williams", "Family Group", "Team Alpha"),
            sampleBodies = listOf(
                "Hey! Are you free for lunch today?\nWe are heading over to the new Italian restaurant near the office around 12:30.\nLet me know if you'd like to join us!",
                "Can you review the latest UI design updates on Figma?\nI added the new segmented buttons, corner radius sliders, and expanded padding options.\nAll test cases passed on Android 15.",
                "Running 10 minutes late to the meeting!\nPlease start the standup without me and go over the Jira sprint board first.",
                "Great job on the release today! 🎉\nCrash-free sessions are up to 99.8% and user ratings jumped to 4.9 stars across all devices.",
                "Sent an attachment: project_specs.pdf\nMake sure to check page 4 for the updated architectural diagram and security tokens."
            ),
            sampleActions = listOf(
                listOf(
                    NotificationActionItem(title = "Reply", isQuickReply = true, actionKey = "wa_reply"),
                    NotificationActionItem(title = "Mark as read", isQuickReply = false, actionKey = "wa_read")
                ),
                listOf(
                    NotificationActionItem(title = "Reply", isQuickReply = true, actionKey = "wa_reply"),
                    NotificationActionItem(title = "Mute", isQuickReply = false, actionKey = "wa_mute")
                )
            )
        ),
        AppPreset(
            appName = "Telegram",
            packageName = "org.telegram.messenger",
            color = 0xFF24A1DE.toInt(),
            iconLetter = "T",
            sampleSenders = listOf("Elena Rostova", "Alexandre Dumas", "Dev Community", "Product Updates"),
            sampleTitles = listOf("Elena Rostova", "Alexandre Dumas", "Dev Community", "Product Updates"),
            sampleBodies = listOf(
                "Check out this new Kotlin Multiplatform library!\nIt supports declarative animations with spring physics and zero boilerplate.\nI think we should migrate our common UI components to it.",
                "Did you test the new animation curves?\nThe bottom-up collapse and dual-end marquee fade look incredibly smooth on high refresh rate screens.",
                "Server deployment finished with 0 errors.\nAll 12 microservices are healthy and database migrations completed successfully in 4.2 seconds.",
                "Let's sync up after the standup call to discuss the Dynamic Island touch boundaries."
            ),
            sampleActions = listOf(
                listOf(
                    NotificationActionItem(title = "Reply", isQuickReply = true, actionKey = "tg_reply"),
                    NotificationActionItem(title = "Mark as read", isQuickReply = false, actionKey = "tg_read")
                )
            )
        ),
        AppPreset(
            appName = "Gmail",
            packageName = "com.google.android.gm",
            color = 0xFFEA4335.toInt(),
            iconLetter = "M",
            sampleSenders = listOf("GitHub", "Google Cloud", "Stripe", "Figma", "Spotify"),
            sampleTitles = listOf(
                "Security alert: New sign-in detected",
                "Your monthly invoice is ready",
                "New comment on 'Island Design Spec'",
                "Your weekly developer digest",
                "Build #482 succeeded on main branch"
            ),
            sampleBodies = listOf(
                "We detected a new sign-in to your Google Account from a macOS device in Colombo, Sri Lanka.\nIf this was you, you don't need to do anything.\nIf not, please review your recent security activity immediately.",
                "Your receipt for Google Cloud Platform Services is now available.\nTotal billed amount: $48.20 for compute engine and cloud storage instances.\nView detailed usage metrics in the console.",
                "Sameera mentioned you in a comment on Frame 4:\n\"I updated the expanded notification card to show up to 7 lines of body text with dynamic font size and M3 Expressive container styling.\"",
                "Explore the top open-source projects trending this week across Kotlin, Jetpack Compose, and Rust.\nOver 2,400 developers joined the community digest.",
                "All 42 test suites passed in 1m 24s.\nDocker container built and deployed to staging environment successfully."
            ),
            sampleActions = listOf(
                listOf(
                    NotificationActionItem(title = "Archive", isQuickReply = false, actionKey = "gm_archive"),
                    NotificationActionItem(title = "Reply", isQuickReply = true, actionKey = "gm_reply")
                ),
                listOf(
                    NotificationActionItem(title = "Delete", isQuickReply = false, actionKey = "gm_delete"),
                    NotificationActionItem(title = "Mark as read", isQuickReply = false, actionKey = "gm_read")
                )
            )
        ),
        AppPreset(
            appName = "GitHub",
            packageName = "com.github.android",
            color = 0xFF24292E.toInt(),
            iconLetter = "G",
            sampleSenders = listOf("GitHub Actions", "Dependabot", "Octocat", "Code Review"),
            sampleTitles = listOf(
                "airsync-android: PR #42 merged",
                "Security vulnerability in gradle wrapper",
                "Review requested on 'Dynamic Island actions'",
                "Workflow run completed: CI / Build"
            ),
            sampleBodies = listOf(
                "sameerasw merged commit 8a1f20 into main.\nBranch feature/island-action-buttons deleted.\nAll unit tests and lint checks verified.",
                "Dependabot created a PR to bump compose-bom to 2026.03.00.\nIncludes Material 3 Expressive performance enhancements and spring physics updates.\nAutomated compatibility tests passed.",
                "Please review the changes requested by the core team on PR #88:\n- Added expanded top padding slider\n- Extended corner radius to 80dp\n- Increased body text capacity to 7 lines",
                "All checks passed successfully.\nArtifact essentials-debug.apk generated and ready for release verification."
            ),
            sampleActions = listOf(
                listOf(
                    NotificationActionItem(title = "View PR", isQuickReply = false, actionKey = "gh_view"),
                    NotificationActionItem(title = "Approve", isQuickReply = false, actionKey = "gh_approve")
                ),
                listOf(
                    NotificationActionItem(title = "Reply", isQuickReply = true, actionKey = "gh_reply")
                )
            )
        ),
        AppPreset(
            appName = "Messages",
            packageName = "com.google.android.apps.messaging",
            color = 0xFF1A73E8.toInt(),
            iconLetter = "SMS",
            sampleSenders = listOf("David Kim", "Bank of Ceylon", "Uber", "Verification"),
            sampleTitles = listOf("David Kim", "Bank OTP", "Uber", "Auth Code"),
            sampleBodies = listOf(
                "Can you pick up the grocery package on your way home?\nI ordered milk, sourdough bread, organic eggs, and some coffee beans from the supermarket down the street.\nThanks a lot!",
                "Your one-time verification code is 849201. Valid for 5 minutes.\nDo not share this code with anyone, including bank representatives.",
                "Your driver is arriving in a White Prius (CAB-1234).\nPlease be at the pickup location at 14th Street in 3 minutes.\nEnjoy your ride!",
                "Hey, where are we meeting tonight?\nLet me know if we are still going to the rooftop lounge or if plans changed."
            ),
            sampleActions = listOf(
                listOf(
                    NotificationActionItem(title = "Reply", isQuickReply = true, actionKey = "msg_reply"),
                    NotificationActionItem(title = "Copy Code", isQuickReply = false, actionKey = "msg_copy")
                ),
                listOf(
                    NotificationActionItem(title = "Mark as read", isQuickReply = false, actionKey = "msg_read")
                )
            )
        ),
        AppPreset(
            appName = "Slack",
            packageName = "com.Slack",
            color = 0xFF4A154B.toInt(),
            iconLetter = "S",
            sampleSenders = listOf("#general", "#dev-android", "Jordan Smith", "#design-critique"),
            sampleTitles = listOf("#general", "#dev-android", "Jordan Smith", "#design-critique"),
            sampleBodies = listOf(
                "Can anyone look at the crash log on Android 15?\nStack trace indicates a NullPointerException in NotificationListener when parsing legacy extras.\nI opened a ticket in Jira #DEV-2041.",
                "Pushed the fix for segmented button corner radii and expanded padding!\nEverything is looking super sharp in dark mode with dynamic accent glows.\nPlease pull the latest changes and test.",
                "Are you attending the sprint retrospective at 2 PM?\nWe'll be discussing the Q3 roadmap, compose migration progress, and upcoming release milestones.\nMeeting link is in the calendar invite.",
                "The new pitch black tokens look super clean on AMOLED displays.\nContrast ratios pass WCAG AAA standards across all components."
            ),
            sampleActions = listOf(
                listOf(
                    NotificationActionItem(title = "Reply", isQuickReply = true, actionKey = "slack_reply"),
                    NotificationActionItem(title = "Open Channel", isQuickReply = false, actionKey = "slack_open")
                )
            )
        )
    )

    private fun generateAppBitmap(letter: String, bgColor: Int): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, 24f, 24f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (letter.length > 1) 32f else 46f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val fontMetrics = textPaint.fontMetrics
        val yPos = (size / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
        canvas.drawText(letter, size / 2f, yPos, textPaint)

        return bitmap
    }

    /**
     * Generates a realistic test notification alert for Island preview
     */
    fun generateRandomNotification(context: Context? = null): ActiveNotificationAlert {
        val preset = apps.random()
        val randomSender = preset.sampleSenders.random()
        val randomTitle = preset.sampleTitles.random()
        val randomBody = preset.sampleBodies.random()
        val actions = preset.sampleActions.randomOrNull() ?: emptyList()
        val mockIcon = generateAppBitmap(preset.iconLetter, preset.color)

        return ActiveNotificationAlert(
            key = "simulated_${preset.packageName}_${System.currentTimeMillis()}",
            packageName = preset.packageName,
            title = randomTitle,
            text = randomBody,
            icon = mockIcon,
            contentIntent = null,
            timestamp = System.currentTimeMillis(),
            appColor = preset.color,
            senderName = randomSender,
            appName = preset.appName,
            appIcon = mockIcon,
            actions = actions
        )
    }
}
