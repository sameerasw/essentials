package com.sameerasw.island.sdk

data class IslandPayload(
    val schema: Int = IslandSchema.VERSION,
    val items: List<IslandItemPayload> = emptyList(),
    val briefCards: List<BriefCardPayload> = emptyList(),
)

data class BriefCardPayload(
    val key: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val iconId: String? = null,
    val tone: String = IslandTone.ACCENT,
    val trailingIconId: String? = null,
    val trailingText: String? = null,
    val trailingTone: String = IslandTone.ACCENT,
    val pageHeightDp: Int? = null,
)

data class IslandItemPayload(
    val key: String = "",
    val compact: List<IslandCellPayload> = emptyList(),
    val priority: Int = 100,
    val priorityOverride: Int? = null,
    val placement: String = IslandPlacement.DYNAMIC,
    val compactVisible: Boolean = true,
    val line: IslandLinePayload? = null,
    val expanded: IslandExpandedPayload? = null,
    val accent: Int? = null,
    val dismissible: Boolean = false,
    val interactions: IslandInteractionsPayload = IslandInteractionsPayload(),
)

data class IslandCellPayload(
    val key: String = "",
    val iconId: String? = null,
    val text: String? = null,
    val tone: String = IslandTone.ACCENT,
    val soloOnly: Boolean = false,
)

data class IslandLinePayload(
    val start: String = "",
    val end: String = "",
    val iconId: String? = null,
    val tone: String = IslandTone.ACCENT,
)

data class IslandExpandedPayload(
    val heightDp: Int = 0,
    val scrollable: Boolean = false,
)

data class IslandInteractionsPayload(
    val onTap: IslandAction? = null,
    val onLongPress: IslandAction? = null,
    val onExpandedTap: IslandAction? = null,
    val onOpen: IslandAction? = null,
)

data class IslandAction(
    val type: String = IslandActionType.NONE,
    val target: String? = null,
)
