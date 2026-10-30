package com.sameerasw.essentials.services

import com.sameerasw.essentials.domain.model.AutoDismissAppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoDismissMathTest {
    private val ownPackage = "com.sameerasw.essentials"
    private val configuredPackage = "com.example.chat"
    private val configs =
        listOf(
            AutoDismissAppConfig(configuredPackage, 15),
            AutoDismissAppConfig("com.example.other", 60),
        )

    @Test
    fun returnsDeadlineForConfiguredPackage() {
        val postTime = 1_000_000L
        val expected = postTime + 15 * 60_000L
        assertEquals(
            expected,
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = postTime,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configs,
                channelId = "general",
            ),
        )
    }

    @Test
    fun returnsNullForUnconfiguredPackage() {
        assertNull(
            autoDismissDeadline(
                packageName = "com.unconfigured.app",
                postTime = 1_000_000L,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configs,
                channelId = "general",
            ),
        )
    }

    @Test
    fun returnsNullForOwnPackage() {
        val configsWithOwn = configs + AutoDismissAppConfig(ownPackage, 10)
        assertNull(
            autoDismissDeadline(
                packageName = ownPackage,
                postTime = 1_000_000L,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configsWithOwn,
                channelId = "general",
            ),
        )
    }

    @Test
    fun returnsNullWhenNotClearable() {
        assertNull(
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = 1_000_000L,
                isClearable = false,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configs,
                channelId = "general",
            ),
        )
    }

    @Test
    fun returnsNullForGroupSummary() {
        assertNull(
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = 1_000_000L,
                isClearable = true,
                isGroupSummary = true,
                ownPackage = ownPackage,
                configs = configs,
                channelId = "general",
            ),
        )
    }

    @Test
    fun allMinutesValuesProduceCorrectDeadlines() {
        val postTime = 5_000_000L
        AUTO_DISMISS_MINUTES.forEach { minutes ->
            val appConfigs = listOf(AutoDismissAppConfig(configuredPackage, minutes))
            val deadline =
                autoDismissDeadline(
                    packageName = configuredPackage,
                    postTime = postTime,
                    isClearable = true,
                    isGroupSummary = false,
                    ownPackage = ownPackage,
                    configs = appConfigs,
                    channelId = "general",
                )
            assertEquals(postTime + minutes * 60_000L, deadline)
        }
        assertEquals(
            postTime + 1 * 60_000L,
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = postTime,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = listOf(AutoDismissAppConfig(configuredPackage, 1)),
                channelId = "general",
            ),
        )
        assertEquals(
            postTime + 1440 * 60_000L,
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = postTime,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = listOf(AutoDismissAppConfig(configuredPackage, 1440)),
                channelId = "general",
            ),
        )
    }

    @Test
    fun returnsNullForExcludedChannel() {
        val configsWithExclusion =
            listOf(
                AutoDismissAppConfig(
                    packageName = configuredPackage,
                    minutes = 15,
                    excludedChannels = listOf("spam", "promotions"),
                ),
            )
        assertNull(
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = 1_000_000L,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configsWithExclusion,
                channelId = "spam",
            ),
        )
    }

    @Test
    fun returnsDeadlineForOtherChannelOfSameApp() {
        val configsWithExclusion =
            listOf(
                AutoDismissAppConfig(
                    packageName = configuredPackage,
                    minutes = 15,
                    excludedChannels = listOf("spam", "promotions"),
                ),
            )
        val postTime = 1_000_000L
        val expected = postTime + 15 * 60_000L
        assertEquals(
            expected,
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = postTime,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configsWithExclusion,
                channelId = "messages",
            ),
        )
    }

    @Test
    fun returnsDeadlineWhenExcludedChannelsIsNull() {
        val configsWithNullExclusions =
            listOf(
                AutoDismissAppConfig(
                    packageName = configuredPackage,
                    minutes = 15,
                    excludedChannels = null,
                ),
            )
        val postTime = 1_000_000L
        val expected = postTime + 15 * 60_000L
        assertEquals(
            expected,
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = postTime,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configsWithNullExclusions,
                channelId = "spam",
            ),
        )
    }

    @Test
    fun returnsDeadlineForNullChannelIdWithExclusionsSet() {
        val configsWithExclusion =
            listOf(
                AutoDismissAppConfig(
                    packageName = configuredPackage,
                    minutes = 15,
                    excludedChannels = listOf("spam"),
                ),
            )
        val postTime = 1_000_000L
        val expected = postTime + 15 * 60_000L
        assertEquals(
            expected,
            autoDismissDeadline(
                packageName = configuredPackage,
                postTime = postTime,
                isClearable = true,
                isGroupSummary = false,
                ownPackage = ownPackage,
                configs = configsWithExclusion,
                channelId = null,
            ),
        )
    }
}
