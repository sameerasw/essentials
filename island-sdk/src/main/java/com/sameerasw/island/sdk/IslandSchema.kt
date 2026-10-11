package com.sameerasw.island.sdk

object IslandSchema {
    const val VERSION = 1

    const val SERVICE_ACTION = "com.sameerasw.island.PROVIDER"
    const val META_LABEL = "com.sameerasw.island.provider.label"
    const val META_DESCRIPTION = "com.sameerasw.island.provider.description"
    const val META_PRIORITY = "com.sameerasw.island.provider.priority"
    const val META_SETTINGS_ACTIVITY = "com.sameerasw.island.provider.settings"

    const val DEFAULT_HOST_PACKAGE = "com.sameerasw.essentials"
    val ALLOWED_HOSTS = setOf(DEFAULT_HOST_PACKAGE)
}

object IslandSurfaceKey {
    private const val BRIEF_PREFIX = "brief."

    fun brief(cardKey: String) = BRIEF_PREFIX + cardKey
    fun isBrief(surfaceKey: String) = surfaceKey.startsWith(BRIEF_PREFIX)
    fun briefCard(surfaceKey: String) = surfaceKey.removePrefix(BRIEF_PREFIX)
}

object IslandTone {
    const val ACCENT = "accent"
    const val ALERT = "alert"
    const val NONE = "none"
}

object IslandPlacement {
    const val PINNED = "pinned"
    const val DYNAMIC = "dynamic"
}

object IslandActionType {
    const val NONE = "none"
    const val OPEN_APP = "open_app"
    const val CALLBACK = "callback"
    const val EXPAND_ITEM = "expand_item"
    const val COLLAPSE = "collapse"
}

object HostRequestType {
    const val PEEK = "peek"
    const val EXPAND = "expand"
    const val COLLAPSE = "collapse"
    const val DISMISS = "dismiss"
    const val OPEN_APP = "open_app"
    const val KEEP_ALIVE = "keep_alive"
    const val TEXT_INPUT = "text_input"
    const val RESIZE = "resize"

    const val ARG_DURATION_MS = "duration_ms"
    const val ARG_STICKY = "sticky"
    const val ARG_TARGET = "target"
    const val ARG_ACTIVE = "active"
    const val ARG_HEIGHT_PX = "height_px"
}
